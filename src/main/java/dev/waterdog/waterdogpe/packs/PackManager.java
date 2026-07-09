/*
 * Copyright 2022 WaterdogTEAM
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

package dev.waterdog.waterdogpe.packs;

import io.netty.buffer.Unpooled;
import lombok.Getter;
import org.cloudburstmc.protocol.bedrock.data.ResourcePackType;
import org.cloudburstmc.protocol.bedrock.packet.*;
import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.event.defaults.ResourcePacksRebuildEvent;
import dev.waterdog.waterdogpe.network.protocol.user.Platform;
import dev.waterdog.waterdogpe.packs.types.ResourcePack;
import dev.waterdog.waterdogpe.packs.types.ZipResourcePack;
import dev.waterdog.waterdogpe.utils.FileUtils;
import org.cloudburstmc.protocol.common.util.Preconditions;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PackManager {

    private static final long CHUNK_SIZE = 1024 * 256;

    private static final PathMatcher ZIP_PACK_MATCHER = FileSystems.getDefault().getPathMatcher("glob:**.{zip,mcpack}");
    private static final ResourcePackStackPacket.Entry EDU_PACK = new ResourcePackStackPacket.Entry("0fba4063-dba1-4281-9b89-ff9390653530", "1.0.0", "");

    private final ProxyServer proxy;
    @Getter
    private final Map<UUID, ResourcePack> packs = new HashMap<>();
    @Getter
    private final Map<String, ResourcePack> packsByIdVer = new HashMap<>();

    @Getter
    private final ResourcePacksInfoPacket packsInfoPacket = new ResourcePacksInfoPacket();
    @Getter
    private final ResourcePacksInfoPacket cdnPacksInfoPacket = new ResourcePacksInfoPacket();
    @Getter
    private final ResourcePackStackPacket stackPacket = new ResourcePackStackPacket();

    private Set<Platform> disabledCdnPlatforms = EnumSet.noneOf(Platform.class);
    private boolean hasCdnPacks;

    public PackManager(ProxyServer proxy) {
        this.proxy = proxy;
    }

    public void clear() {
        for (ResourcePack pack : this.packs.values()) {
            try {
                pack.close();
            } catch (IOException ignored) {}
        }
        this.packs.clear();
        this.packsByIdVer.clear();
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

        this.loadCdnPacks(packsDirectory.resolveSibling("packs_cdn_cache"));

        this.rebuildPackets();
        this.proxy.getLogger().info("Loaded " + this.packs.size() + " packs!");
    }

    private void loadCdnPacks(Path cacheDirectory) {
        this.disabledCdnPlatforms = this.parseDisabledCdnPlatforms();

        for (String url : this.proxy.getConfiguration().getPackCdnUrls()) {
            try {
                Path packPath = this.downloadCdnPack(url, cacheDirectory);
                if (packPath == null) {
                    continue;
                }

                ResourcePack pack = this.loadPack(packPath, ZipResourcePack.class);
                if (pack == null) {
                    this.proxy.getLogger().error("CDN resource pack from " + url + " has invalid or missing manifest.json!");
                    continue;
                }

                if (this.packs.containsKey(pack.getPackId())) {
                    this.proxy.getLogger().warning("CDN resource pack from " + url + " has the same UUID as an already loaded pack ("
                            + pack.getPackId() + ")! Skipping the CDN pack. Remove the local copy from the packs folder to serve it from the CDN.");
                    pack.close();
                    continue;
                }

                pack.setCdnUrl(url);
                this.packsByIdVer.put(pack.getPackId() + "_" + pack.getVersion(), pack);
                this.packs.put(pack.getPackId(), pack);
                this.proxy.getLogger().info("Loaded CDN resource pack " + pack.getPackName() + " (" + pack.getPackId() + ") from " + url);
            } catch (Exception e) {
                this.proxy.getLogger().error("Can not load CDN resource pack from " + url, e);
            }
        }
    }

    /**
     * Downloads the pack so its manifest, size and hash are known and chunked
     * transfer stays available as fallback. If the download fails but a copy from
     * a previous start exists in the cache, that copy is used instead.
     */
    private Path downloadCdnPack(String url, Path cacheDirectory) throws IOException {
        Files.createDirectories(cacheDirectory);
        Path packPath = cacheDirectory.resolve(this.hashUrl(url) + ".zip");
        Path downloadPath = cacheDirectory.resolve(this.hashUrl(url) + ".zip.part");

        try {
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(15))
                    .build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofMinutes(2))
                    .build();

            HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(downloadPath,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE));
            if (response.statusCode() != 200) {
                throw new IOException("Server responded with status code " + response.statusCode());
            }
            Files.move(downloadPath, packPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            Files.deleteIfExists(downloadPath);
            if (Files.exists(packPath)) {
                this.proxy.getLogger().warning("Failed to download CDN resource pack from " + url + ": " + e.getMessage()
                        + "! Using the copy cached from a previous start. Note that clients will still download from the CDN, so the URL must serve the same pack once reachable again.");
                return packPath;
            }
            this.proxy.getLogger().error("Failed to download CDN resource pack from " + url + " and no cached copy exists! Skipping this pack.", e);
            return null;
        }
        return packPath;
    }

    private String hashUrl(String url) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(url.getBytes(StandardCharsets.UTF_8));
            return new BigInteger(1, digest).toString(16);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash pack url", e);
        }
    }

    private Set<Platform> parseDisabledCdnPlatforms() {
        Set<Platform> platforms = EnumSet.noneOf(Platform.class);
        for (String name : this.proxy.getConfiguration().getDisableCdnPlatforms()) {
            try {
                platforms.add(Platform.valueOf(name.trim().toUpperCase().replace(' ', '_')));
            } catch (IllegalArgumentException e) {
                this.proxy.getLogger().warning("Unknown platform " + name + " in disable_cdn_for config option!");
            }
        }
        return platforms;
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
            this.proxy.getLogger().error("Resource pack manifest.json is invalid or was not found in " + packPath.getFileName() + ", please make sure that you zip the content of the pack and not the folder! Read more on troubleshooting here: https://docs.waterdog.dev/books/waterdogpe-setup/page/troubleshooting");
        } catch (Exception e) {
            this.proxy.getLogger().error("Can not load resource pack from: " + packPath.getFileName(), e);
        }
        return null;
    }

    private ResourcePack loadPack(Path packPath, Class<? extends ResourcePack> clazz) throws Exception {
        ResourcePack pack = clazz.getDeclaredConstructor(Path.class).newInstance(packPath);
        if (!pack.loadManifest() || !pack.getPackManifest().validate()) {
            return null;
        }

        File contentKeyFile = new File(packPath.getParent().toFile(), packPath.toFile().getName() + ".key");
        pack.setContentKey(contentKeyFile.exists() ? Files.readString(contentKeyFile.toPath(), StandardCharsets.UTF_8).replace("\n", "") : "");

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
        try {
            resourcePack.close();
        } catch (IOException ignored) {}

        String packIdVer = resourcePack.getPackId() + "_" + resourcePack.getVersion();
        this.packsByIdVer.remove(packIdVer);
        this.rebuildPackets();
        return true;
    }

    public void rebuildPackets() {
        this.packsInfoPacket.setForcedToAccept(this.proxy.getConfiguration().isForceServerPacks());
        this.packsInfoPacket.setWorldTemplateId(UUID.randomUUID());
        this.packsInfoPacket.setWorldTemplateVersion("");
        this.cdnPacksInfoPacket.setForcedToAccept(this.packsInfoPacket.isForcedToAccept());
        this.cdnPacksInfoPacket.setWorldTemplateId(this.packsInfoPacket.getWorldTemplateId());
        this.cdnPacksInfoPacket.setWorldTemplateVersion(this.packsInfoPacket.getWorldTemplateVersion());
        this.stackPacket.setForcedToAccept(this.proxy.getConfiguration().isOverwriteClientPacks());

        this.packsInfoPacket.getBehaviorPackInfos().clear();
        this.packsInfoPacket.getResourcePackInfos().clear();

        this.cdnPacksInfoPacket.getBehaviorPackInfos().clear();
        this.cdnPacksInfoPacket.getResourcePackInfos().clear();

        this.stackPacket.getBehaviorPacks().clear();
        this.stackPacket.getResourcePacks().clear();

        this.stackPacket.setGameVersion("");

        this.hasCdnPacks = false;
        for (ResourcePack pack : this.packs.values()) {
            ResourcePacksInfoPacket.Entry infoEntry = this.createInfoEntry(pack, null);
            // Separate entry instance carrying the CDN URL, so mutating one packet never leaks into the other
            ResourcePacksInfoPacket.Entry cdnInfoEntry = this.createInfoEntry(pack, pack.getCdnUrl());
            ResourcePackStackPacket.Entry stackEntry = new ResourcePackStackPacket.Entry(pack.getPackId().toString(), pack.getVersion().toString(), "");
            if (pack.getCdnUrl() != null) {
                this.hasCdnPacks = true;
            }
            if (pack.getType().equals(ResourcePack.TYPE_RESOURCES)) {
                this.packsInfoPacket.getResourcePackInfos().add(infoEntry);
                this.cdnPacksInfoPacket.getResourcePackInfos().add(cdnInfoEntry);
                this.stackPacket.getResourcePacks().add(stackEntry);
            } else if (pack.getType().equals(ResourcePack.TYPE_DATA)) {
                this.packsInfoPacket.getBehaviorPackInfos().add(infoEntry);
                this.cdnPacksInfoPacket.getBehaviorPackInfos().add(cdnInfoEntry);
                this.stackPacket.getBehaviorPacks().add(stackEntry);
            }
        }

        if (this.proxy.getConfiguration().enableEducationFeatures()) {
            this.stackPacket.getBehaviorPacks().add(EDU_PACK);
        }
        ResourcePacksRebuildEvent event = new ResourcePacksRebuildEvent(this.packsInfoPacket, this.cdnPacksInfoPacket, this.stackPacket);
        this.proxy.getEventManager().callEvent(event);
    }

    private ResourcePacksInfoPacket.Entry createInfoEntry(ResourcePack pack, String cdnUrl) {
        return new ResourcePacksInfoPacket.Entry(pack.getPackId(), pack.getVersion().toString(),
                pack.getPackSize(), pack.getContentKey(), "", pack.getContentKey().isEmpty() ? "" : pack.getPackId().toString(), false, false, false, cdnUrl);
    }

    /**
     * Returns the ResourcePacksInfoPacket which should be sent to a player on the given platform.
     * Platforms listed in the disable_cdn_for config option always receive the packet without
     * CDN URLs, causing them to use the in-protocol chunked transfer.
     */
    public ResourcePacksInfoPacket getPacksInfoPacket(Platform platform) {
        if (!this.hasCdnPacks || this.disabledCdnPlatforms.contains(platform)) {
            return this.packsInfoPacket;
        }
        return this.cdnPacksInfoPacket;
    }

    public ResourcePackDataInfoPacket packInfoFromIdVer(String idVersion) {
        ResourcePack resourcePack = this.packsByIdVer.get(idVersion);
        if (resourcePack == null) {
            return null;
        }

        ResourcePackDataInfoPacket packet = new ResourcePackDataInfoPacket();
        packet.setPackId(resourcePack.getPackId());
        packet.setPackVersion(resourcePack.getVersion().toString());
        packet.setMaxChunkSize(CHUNK_SIZE);
        packet.setChunkCount((resourcePack.getPackSize() - 1) / packet.getMaxChunkSize() + 1);
        packet.setCompressedPackSize(resourcePack.getPackSize());
        packet.setHash(resourcePack.getHash());
        if (resourcePack.getType().equals(ResourcePack.TYPE_RESOURCES)) {
            packet.setType(ResourcePackType.RESOURCES);
        } else if (resourcePack.getType().equals(ResourcePack.TYPE_DATA)) {
            packet.setType(ResourcePackType.ADDON);
        }
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
        packet.setData(Unpooled.wrappedBuffer(resourcePack.getChunk((int) CHUNK_SIZE * from.getChunkIndex(), (int) CHUNK_SIZE)));
        packet.setProgress(CHUNK_SIZE * from.getChunkIndex());
        return packet;
    }

}
