/*
 * Copyright 2021 WaterdogTEAM
 * Licensed under the GNU General Public License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.waterdog.packs;

import com.google.common.base.Preconditions;
import com.nukkitx.protocol.bedrock.data.ResourcePackType;
import com.nukkitx.protocol.bedrock.packet.*;
import dev.waterdog.ProxyServer;
import dev.waterdog.event.defaults.ResourcePacksRebuildEvent;
import dev.waterdog.packs.types.ResourcePack;
import dev.waterdog.packs.types.ZipResourcePack;
import dev.waterdog.utils.FileUtils;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PackManager {

    private static final PathMatcher ZIP_PACK_MATCHER = FileSystems.getDefault().getPathMatcher("glob:**.{zip,mcpack}");

    private final ProxyServer proxy;
    private final Map<UUID, ResourcePack> packs = new HashMap<>();
    private final Map<String, ResourcePack> packsByIdVer = new HashMap<>();

    private final ResourcePacksInfoPacket packsInfoPacket = new ResourcePacksInfoPacket();
    private final ResourcePackStackPacket stackPacket = new ResourcePackStackPacket();

    public PackManager(ProxyServer proxy) {
        this.proxy = proxy;
    }

    public void loadPacks(Path packsDirectory) {
        Preconditions.checkNotNull(packsDirectory, "Packs directory can not be null!");
        Preconditions.checkArgument(Files.isDirectory(packsDirectory), packsDirectory + " must be directory!");
        this.proxy.getLogger().info("Loading resource packs!");

        try {
            DirectoryStream<Path> stream = Files.newDirectoryStream(packsDirectory);
            for (Path path : stream) {
                ResourcePack resourcePack = this.constructPack(path);
                if (resourcePack != null) {
                    String packIdVer = resourcePack.getPackId() + "_" + resourcePack.getPackManifest().getHeader().getVersion();
                    this.packsByIdVer.put(packIdVer, resourcePack);
                    this.packs.put(resourcePack.getPackId(), resourcePack);
                }
            }
        } catch (IOException e) {
            this.proxy.getLogger().error("Can not load packs!", e);
        }

        this.rebuildPackets();
        this.proxy.getLogger().info("Loaded " + this.packs.size() + " packs!");
    }

    private ResourcePack constructPack(Path packPath) {
        Class<? extends ResourcePack> loader = this.getPackLoader(packPath);
        if (loader == null) {
            return null;
        }

        try {
            ResourcePack pack = this.loadPack(packPath, loader);
            if (pack != null) {
                return pack;
            }
            this.proxy.getLogger().error("Resource pack has invalid manifest file!");
        } catch (Exception e) {
            this.proxy.getLogger().error("Can not load resource pack!", e);
        }
        return null;
    }

    private ResourcePack loadPack(Path packPath, Class<? extends ResourcePack> clazz) throws Exception {
        ResourcePack pack = clazz.getDeclaredConstructor(Path.class).newInstance(packPath);
        try {
            pack.loadManifest();
        } catch (IOException e) {
            throw new IOException("Can not load manifest!");
        }

        if (!pack.getPackManifest().validate()) {
            return null;
        }

        if (this.proxy.getConfiguration().getPackCacheSize() >= (pack.getPackSize() / FileUtils.INT_MEGABYTE)) {
            pack.saveToCache();
        }
        return pack;
    }

    /**
     * We are currently supporting only zipped resource packs
     *
     * @param path to resource pack.
     * @return class which will be used to load pack.
     */
    public Class<? extends ResourcePack> getPackLoader(Path path) {
        if (ZIP_PACK_MATCHER.matches(path)) {
            return ZipResourcePack.class;
        }
        return null;
    }

    public boolean registerPack(ResourcePack resourcePack) {
        Preconditions.checkNotNull(resourcePack, "Resource pack can not be null!");
        Preconditions.checkArgument(resourcePack.getPackManifest().validate(), "Resource pack has invalid manifest!");

        if (this.packs.get(resourcePack.getPackId()) != null) {
            return false;
        }

        String packIdVer = resourcePack.getPackId() + "_" + resourcePack.getVersion();
        this.packsByIdVer.put(packIdVer, resourcePack);
        this.packs.put(resourcePack.getPackId(), resourcePack);
        this.rebuildPackets();
        return true;
    }

    public boolean unregisterPack(UUID packId) {
        ResourcePack resourcePack = this.packs.remove(packId);
        if (resourcePack == null) {
            return false;
        }

        String packIdVer = resourcePack.getPackId() + "_" + resourcePack.getVersion();
        this.packsByIdVer.remove(packIdVer);
        this.rebuildPackets();
        return true;
    }

    public void rebuildPackets() {
        boolean forcePacks = this.proxy.getConfiguration().forcePacks();
        this.packsInfoPacket.setForcedToAccept(forcePacks);
        this.stackPacket.setForcedToAccept(forcePacks);

        this.packsInfoPacket.getBehaviorPackInfos().clear();
        this.packsInfoPacket.getResourcePackInfos().clear();

        this.stackPacket.getBehaviorPacks().clear();
        this.stackPacket.getResourcePacks().clear();
        this.stackPacket.setGameVersion("");

        for (ResourcePack pack : this.packs.values()) {
            if (!pack.getType().equals(ResourcePack.TYPE_RESOURCES)) {
                continue;
            }
            ResourcePacksInfoPacket.Entry infoEntry = new ResourcePacksInfoPacket.Entry(pack.getPackId().toString(), pack.getVersion().toString(),
                    pack.getPackSize(), "", "", "", false, false);
            this.packsInfoPacket.getResourcePackInfos().add(infoEntry);
            ResourcePackStackPacket.Entry stackEntry = new ResourcePackStackPacket.Entry(pack.getPackId().toString(), pack.getVersion().toString(), "");
            this.stackPacket.getResourcePacks().add(stackEntry);
        }

        ResourcePacksRebuildEvent event = new ResourcePacksRebuildEvent(this.packsInfoPacket, this.stackPacket);
        this.proxy.getEventManager().callEvent(event);
    }

    public ResourcePackDataInfoPacket packInfoFromIdVer(String idVersion) {
        ResourcePack resourcePack = this.packsByIdVer.get(idVersion);
        if (resourcePack == null) {
            return null;
        }

        ResourcePackDataInfoPacket packet = new ResourcePackDataInfoPacket();
        packet.setPackId(resourcePack.getPackId());
        packet.setPackVersion(resourcePack.getVersion().toString());
        packet.setMaxChunkSize(FileUtils.INT_MEGABYTE);
        packet.setChunkCount(resourcePack.getPackSize() / packet.getMaxChunkSize());
        packet.setCompressedPackSize(resourcePack.getPackSize());
        packet.setHash(resourcePack.getHash());
        packet.setType(ResourcePackType.RESOURCE);
        return packet;
    }

    public ResourcePackChunkDataPacket packChunkDataPacket(String idVersion, ResourcePackChunkRequestPacket from) {
        ResourcePack resourcePack = this.packsByIdVer.get(idVersion);
        if (resourcePack == null) {
            return null;
        }

        ResourcePackChunkDataPacket packet = new ResourcePackChunkDataPacket();
        packet.setPackId(from.getPackId());
        packet.setPackVersion(from.getPackVersion());
        packet.setChunkIndex(from.getChunkIndex());
        packet.setData(resourcePack.getChunk(FileUtils.INT_MEGABYTE * from.getChunkIndex(), FileUtils.INT_MEGABYTE));
        packet.setProgress(FileUtils.INT_MEGABYTE * from.getChunkIndex());
        return packet;
    }

    public ResourcePacksInfoPacket getPacksInfoPacket() {
        return this.packsInfoPacket;
    }

    public ResourcePackStackPacket getStackPacket() {
        return this.stackPacket;
    }

    public Map<UUID, ResourcePack> getPacks() {
        return this.packs;
    }

    public Map<String, ResourcePack> getPacksByIdVer() {
        return this.packsByIdVer;
    }
}
