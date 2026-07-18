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
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PackManager {

    private static final long CHUNK_SIZE = 1024 * 256;

    private static final PathMatcher ZIP_PACK_MATCHER = FileSystems.getDefault().getPathMatcher("glob:**.{zip,mcpack}");
    private static final ResourcePackStackPacket.Entry EDU_PACK = new ResourcePackStackPacket.Entry("0fba4063-dba1-4281-9b89-ff9390653530", "1.0.0", "");

    private final ProxyServer proxy;
    // Rebuilt and swapped in as a whole on (re)load so joining players read a complete, consistent set.
    @Getter
    private Map<UUID, ResourcePack> packs = new HashMap<>();
    @Getter
    private Map<String, ResourcePack> packsByIdVer = new HashMap<>();

    @Getter
    private ResourcePacksInfoPacket packsInfoPacket = new ResourcePacksInfoPacket();
    @Getter
    private ResourcePackStackPacket stackPacket = new ResourcePackStackPacket();

    private ResourcePacksInfoPacket noCdnPacksInfoPacket;

    private Set<Platform> disabledCdnPlatforms = EnumSet.noneOf(Platform.class);

    // Directories loadPacks has been called with; reloadPacks re-scans all of them.
    private final List<Path> packDirectories = new ArrayList<>();

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

    /**
     * Registers a packs directory and loads packs, swapping them in for players who connect from now on;
     * players already past the resource pack stage keep what they were sent. May be called more than
     * once to serve packs from several directories. When pack_cdn_urls is empty the registered folders
     * are served, otherwise the CDN packs are.
     */
    public void loadPacks(Path packsDirectory) {
        Preconditions.checkNotNull(packsDirectory, "Packs directory can not be null!");
        Preconditions.checkArgument(Files.isDirectory(packsDirectory), packsDirectory + " must be directory!");
        if (!this.packDirectories.contains(packsDirectory)) {
            this.packDirectories.add(packsDirectory);
        }
        this.buildPacks();
    }

    /**
     * Reloads packs from every directory {@link #loadPacks(Path)} was called with, picking up config
     * changes and refreshing pack contents for players who connect from now on.
     */
    public void reloadPacks() {
        this.buildPacks();
    }

    private void buildPacks() {
        this.proxy.getLogger().info("Loading resource packs!");

        Path cacheDirectory = this.cdnCacheDirectory();
        Set<Platform> disabled = this.parseDisabledCdnPlatforms();
        Map<UUID, ResourcePack> newPacks = new HashMap<>();
        Map<String, ResourcePack> newPacksByIdVer = new HashMap<>();

        List<String> cdnUrls = this.proxy.getConfiguration().getPackCdnUrls();
        if (cdnUrls.isEmpty()) {
            for (Path directory : this.packDirectories) {
                this.loadLocalPacks(directory, newPacks, newPacksByIdVer);
            }
        } else if (cacheDirectory != null) {
            this.loadCdnPacks(cacheDirectory, cdnUrls, newPacks, newPacksByIdVer);
        }

        // Swap the freshly built set in first, then dispose the old one, so an in-flight chunk request
        // is already reading the new packs before we close the packs it replaced.
        Map<UUID, ResourcePack> oldPacks = this.packs;
        this.disabledCdnPlatforms = disabled;
        this.packs = newPacks;
        this.packsByIdVer = newPacksByIdVer;
        this.rebuildPackets();

        for (ResourcePack pack : oldPacks.values()) {
            try {
                pack.close();
            } catch (IOException ignored) {}
        }

        // With the replaced packs closed, drop any cache files no longer backing a live pack (older
        // downloads, and everything if we just switched from CDN back to local packs).
        if (cacheDirectory != null) {
            this.cleanupCdnCache(cacheDirectory, newPacks.values());
        }

        this.proxy.getLogger().info("Loaded " + this.packs.size() + " packs!");
    }

    private Path cdnCacheDirectory() {
        return this.packDirectories.isEmpty() ? null : this.packDirectories.get(0).resolveSibling("packs_cdn_cache");
    }

    private void loadLocalPacks(Path packsDirectory, Map<UUID, ResourcePack> packs, Map<String, ResourcePack> packsByIdVer) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(packsDirectory)) {
            for (Path path : stream) {
                ResourcePack resourcePack = this.constructPack(path);
                if (resourcePack != null) {
                    packsByIdVer.put(resourcePack.getPackId() + "_" + resourcePack.getPackManifest().getHeader().getVersion(), resourcePack);
                    packs.put(resourcePack.getPackId(), resourcePack);
                }
            }
        } catch (IOException e) {
            this.proxy.getLogger().error("Can not load packs!", e);
        }
    }

    private void loadCdnPacks(Path cacheDirectory, List<String> urls, Map<UUID, ResourcePack> packs, Map<String, ResourcePack> packsByIdVer) {
        for (String url : urls) {
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

                if (packs.containsKey(pack.getPackId())) {
                    this.proxy.getLogger().warning("CDN resource pack from " + url + " has the same UUID as another CDN pack ("
                            + pack.getPackId() + ")! Skipping the duplicate.");
                    pack.close();
                    continue;
                }

                pack.setCdnUrl(url);
                packsByIdVer.put(pack.getPackId() + "_" + pack.getVersion(), pack);
                packs.put(pack.getPackId(), pack);
                this.proxy.getLogger().info("Loaded CDN resource pack " + pack.getPackName() + " (" + pack.getPackId() + ") from " + url);
            } catch (Exception e) {
                this.proxy.getLogger().error("Can not load CDN resource pack from " + url, e);
            }
        }
    }

    private void cleanupCdnCache(Path cacheDirectory, Collection<ResourcePack> livePacks) {
        if (!Files.isDirectory(cacheDirectory)) {
            return;
        }

        Set<String> keep = new HashSet<>();
        for (ResourcePack pack : livePacks) {
            if (pack.getCdnUrl() != null) {
                keep.add(pack.getPackPath().getFileName().toString());
            }
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(cacheDirectory)) {
            for (Path path : stream) {
                if (keep.contains(path.getFileName().toString())) {
                    continue;
                }
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {}
            }
        } catch (IOException e) {
            this.proxy.getLogger().warning("Failed to clean up CDN pack cache: " + e.getMessage());
        }
    }

    /**
     * Downloads the pack so its manifest, size and hash are known and chunked transfer stays available
     * as fallback. Each download lands in a uniquely named file so it never collides with a file the
     * pack being replaced still holds open. If the download fails but a copy from an earlier load or a
     * previous start exists in the cache, the newest such copy is used instead.
     */
    private Path downloadCdnPack(String url, Path cacheDirectory) throws IOException {
        Files.createDirectories(cacheDirectory);
        String prefix = this.hashUrl(url);
        // Unique per download (as Geyser does) so a fresh download never has to overwrite a file that a
        // pack being replaced still holds open, which Windows would refuse.
        String fileName = prefix + "." + System.currentTimeMillis();
        Path packPath = cacheDirectory.resolve(fileName + ".zip");
        Path downloadPath = cacheDirectory.resolve(fileName + ".zip.part");

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
            Path cached = this.findCachedPack(cacheDirectory, prefix);
            if (cached != null) {
                this.proxy.getLogger().warning("Failed to download CDN resource pack from " + url + ": " + e.getMessage()
                        + "! Using a previously cached copy. Note that clients will still download from the CDN, so the URL must serve the same pack once reachable again.");
                return cached;
            }
            this.proxy.getLogger().error("Failed to download CDN resource pack from " + url + " and no cached copy exists! Skipping this pack.", e);
            return null;
        }
        return packPath;
    }

    /**
     * Finds the most recently cached copy of a pack (from any earlier download) for the given url hash,
     * used as a fallback when a fresh download fails.
     */
    private Path findCachedPack(Path cacheDirectory, String prefix) {
        Path newest = null;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(cacheDirectory, prefix + ".*.zip")) {
            long newestTime = Long.MIN_VALUE;
            for (Path path : stream) {
                long time = Files.getLastModifiedTime(path).toMillis();
                if (time > newestTime) {
                    newestTime = time;
                    newest = path;
                }
            }
        } catch (IOException ignored) {}
        return newest;
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
        ResourcePacksInfoPacket infoPacket = new ResourcePacksInfoPacket();
        infoPacket.setForcedToAccept(this.proxy.getConfiguration().isForceServerPacks());
        infoPacket.setVibrantVisualsForceDisabled(this.proxy.getConfiguration().isForceDisableVibrantVisuals());
        infoPacket.setWorldTemplateId(UUID.randomUUID());
        infoPacket.setWorldTemplateVersion("");

        ResourcePackStackPacket stackPacket = new ResourcePackStackPacket();
        stackPacket.setForcedToAccept(this.proxy.getConfiguration().isOverwriteClientPacks());
        stackPacket.setGameVersion("");

        boolean hasCdnPacks = false;
        for (ResourcePack pack : this.packs.values()) {
            // The CDN URL goes straight into the packet entry; disabled platforms get a stripped copy below.
            ResourcePacksInfoPacket.Entry infoEntry = this.createInfoEntry(pack, pack.getCdnUrl());
            ResourcePackStackPacket.Entry stackEntry = new ResourcePackStackPacket.Entry(pack.getPackId().toString(), pack.getVersion().toString(), "");
            if (pack.getCdnUrl() != null) {
                hasCdnPacks = true;
            }
            if (pack.getType().equals(ResourcePack.TYPE_RESOURCES)) {
                infoPacket.getResourcePackInfos().add(infoEntry);
                stackPacket.getResourcePacks().add(stackEntry);
            } else if (pack.getType().equals(ResourcePack.TYPE_DATA)) {
                infoPacket.getBehaviorPackInfos().add(infoEntry);
                stackPacket.getBehaviorPacks().add(stackEntry);
            }
        }

        if (this.proxy.getConfiguration().enableEducationFeatures()) {
            stackPacket.getBehaviorPacks().add(EDU_PACK);
        }
        ResourcePacksRebuildEvent event = new ResourcePacksRebuildEvent(infoPacket, stackPacket);
        this.proxy.getEventManager().callEvent(event);

        this.noCdnPacksInfoPacket = (hasCdnPacks && !this.disabledCdnPlatforms.isEmpty())
                ? this.buildNoCdnPacket(infoPacket) : null;
        // Swap the freshly built packets in last so joining players never observe a half-built packet.
        this.packsInfoPacket = infoPacket;
        this.stackPacket = stackPacket;
    }

    private ResourcePacksInfoPacket.Entry createInfoEntry(ResourcePack pack, String cdnUrl) {
        return new ResourcePacksInfoPacket.Entry(pack.getPackId(), pack.getVersion().toString(),
                pack.getPackSize(), pack.getContentKey(), "", pack.getContentKey().isEmpty() ? "" : pack.getPackId().toString(), false, false, false, cdnUrl);
    }

    /**
     * Builds a copy of packsInfoPacket with the CDN URL removed from every entry, so clients on
     * platforms in disable_cdn_for fall back to the in-protocol chunked transfer.
     */
    private ResourcePacksInfoPacket buildNoCdnPacket(ResourcePacksInfoPacket source) {
        ResourcePacksInfoPacket packet = new ResourcePacksInfoPacket();
        packet.setForcedToAccept(source.isForcedToAccept());
        packet.setVibrantVisualsForceDisabled(source.isVibrantVisualsForceDisabled());
        packet.setWorldTemplateId(source.getWorldTemplateId());
        packet.setWorldTemplateVersion(source.getWorldTemplateVersion());
        for (ResourcePacksInfoPacket.Entry entry : source.getResourcePackInfos()) {
            packet.getResourcePackInfos().add(this.stripCdnUrl(entry));
        }
        for (ResourcePacksInfoPacket.Entry entry : source.getBehaviorPackInfos()) {
            packet.getBehaviorPackInfos().add(this.stripCdnUrl(entry));
        }
        return packet;
    }

    private ResourcePacksInfoPacket.Entry stripCdnUrl(ResourcePacksInfoPacket.Entry entry) {
        return new ResourcePacksInfoPacket.Entry(entry.getPackId(), entry.getPackVersion(), entry.getPackSize(),
                entry.getContentKey(), entry.getSubPackName(), entry.getContentId(), entry.isScripting(),
                entry.isRaytracingCapable(), entry.isAddonPack(), null);
    }

    /**
     * Returns the ResourcePacksInfoPacket which should be sent to a player on the given platform.
     * Platforms listed in the disable_cdn_for config option receive the packet without CDN URLs,
     * causing them to use the in-protocol chunked transfer.
     */
    public ResourcePacksInfoPacket getPacksInfoPacket(Platform platform) {
        if (this.noCdnPacksInfoPacket != null && this.disabledCdnPlatforms.contains(platform)) {
            return this.noCdnPacksInfoPacket;
        }
        return this.packsInfoPacket;
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
