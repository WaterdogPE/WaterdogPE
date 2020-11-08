/**
 * Copyright 2020 WaterdogTEAM
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pe.waterdog.utils;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import pe.waterdog.logger.MainLogger;
import pe.waterdog.network.ServerInfo;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProxyConfig extends YamlConfig {

    private String motd;
    private int maxPlayerCount;

    private final boolean onlineMode;
    private final boolean replaceUsernameSpaces;
    private final boolean fastCodec;
    private final boolean debug;
    private final boolean injectCommands;
    private final boolean enableResourcePacks;
    private boolean useLoginExtras;
    private boolean enableQuery;
    private boolean ipForward;
    private boolean fastTransfer;
    protected boolean forcePacks;

    private final InetSocketAddress bindAddress;
    private final List<String> priorities;
    private final Map<String, String> forcedHosts;

    private final Object2ObjectOpenHashMap<String, List<String>> playerPermissions = new Object2ObjectOpenHashMap<>();
    private List<String> defaultPermissions;

    private final int upstreamCompression;
    private final int downstreamCompression;
    private int packCacheSize;

    public ProxyConfig(File file){
        super(file);

        this.motd = this.getString("listener.motd");
        this.maxPlayerCount = this.getInt("listener.max_players");
        this.useLoginExtras = this.getBoolean("use_login_extras");
        this.onlineMode = this.getBoolean("online_mode");
        this.enableQuery = this.getBoolean("enable_query");
        this.ipForward = this.getBoolean("ip_forward");
        this.replaceUsernameSpaces = this.getBoolean("replace_username_spaces");
        this.fastCodec = this.getBoolean("use_fast_codec");
        this.fastTransfer = this.getBoolean("prefer_fast_transfer");
        this.debug = this.getBoolean("enable_debug");
        this.injectCommands = this.getBoolean("inject_proxy_commands");
        this.bindAddress = this.getInetAddress("listener.host");
        this.priorities = this.getStringList("listener.priorities");
        this.defaultPermissions = this.getStringList("permissions_default");
        this.playerPermissions.putAll(this.getPlayerPermissions("permissions"));
        this.forcedHosts = (Map<String, String>) this.get("listener.forced_hosts", new Object2ObjectOpenHashMap<>());
        this.upstreamCompression = this.getInt("upstream_compression_level");
        this.downstreamCompression = this.getInt("downstream_compression_level");
        this.enableResourcePacks = this.getBoolean("enable_packs");
        this.forcePacks = this.getBoolean("force_apply_packs");
        this.packCacheSize = this.getInt("pack_cache_size");
    }

    public InetSocketAddress getInetAddress(String key) {
        String addressString = this.getString(key);
        if (addressString == null) return null;

        String[] data = addressString.split(":");
        return new InetSocketAddress(data[0], (data.length <= 1 ? 19132 : Integer.parseInt(data[1])));
    }

    public Map<String, ServerInfo> buildServerMap() {
        Map<String, Map<String, String>> map = (Map<String, Map<String, String>>) this.get("servers");
        Map<String, ServerInfo> servers = new HashMap<>();

        for (String server : map.keySet()) {
            Map<String, String> serverData = map.get(server);

            InetSocketAddress address;
            InetSocketAddress publicAddress = null;

            try {
                String[] data = serverData.get("address").split(":");
                address = new InetSocketAddress(data[0], Integer.parseInt(data[1]));
            } catch (Exception e) {
                MainLogger.getLogger().error("Unable to parse server from config! Please check you configuration. Server name: " + server);
                continue;
            }

            if (serverData.containsKey("public_address")){
                try {
                    String[] data = serverData.get("public_address").split(":");
                    publicAddress = new InetSocketAddress(data[0], Integer.parseInt(data[1]));
                }catch (Exception e){
                    MainLogger.getLogger().warning("Can not parse public server address! Server name: "+server);
                }
            }

            ServerInfo serverInfo = new ServerInfo(server.toLowerCase(), address, publicAddress == null? address : publicAddress);
            servers.put(server.toLowerCase(), serverInfo);
        }
        return servers;
    }

    private Map<String, List<String>> getPlayerPermissions(String key){
        return (Map<String, List<String>>) this.get(key);
    }

    public String getMotd() {
        return this.motd;
    }

    public void setMotd(String motd) {
        this.motd = motd;
    }

    public int getMaxPlayerCount() {
        return this.maxPlayerCount;
    }

    public void setMaxPlayerCount(int maxPlayerCount) {
        this.maxPlayerCount = maxPlayerCount;
    }

    public boolean isOnlineMode() {
        return this.onlineMode;
    }

    public void setEnableQuery(boolean enableQuery) {
        this.enableQuery = enableQuery;
    }

    public boolean isEnabledQuery() {
        return this.enableQuery;
    }

    public boolean useLoginExtras() {
        return this.useLoginExtras;
    }

    public void setUseLoginExtras(boolean useLoginExtras) {
        this.useLoginExtras = useLoginExtras;
    }

    public boolean isReplaceUsernameSpaces() {
        return this.replaceUsernameSpaces;
    }

    public boolean useFastCodec() {
        return this.fastCodec;
    }

    public void setUseFastTransfer(boolean fastTransfer) {
        this.fastTransfer = fastTransfer;
    }

    public boolean useFastTransfer() {
        return this.fastTransfer;
    }

    public boolean isIpForward() {
        return this.ipForward;
    }

    public void setIpForward(boolean ipForward) {
        this.ipForward = ipForward;
    }

    public InetSocketAddress getBindAddress() {
        return this.bindAddress;
    }

    public List<String> getPriorities() {
        return this.priorities;
    }

    public Map<String, String> getForcedHosts() {
        return this.forcedHosts;
    }

    public Map<String, List<String>> getPlayerPermissions() {
        return this.playerPermissions;
    }

    public void setDefaultPermissions(List<String> defaultPermissions) {
        this.defaultPermissions = defaultPermissions;
    }

    public List<String> getDefaultPermissions() {
        return this.defaultPermissions;
    }

    public int getUpstreamCompression() {
        return this.upstreamCompression;
    }

    public int getDownstreamCompression() {
        return this.downstreamCompression;
    }

    public boolean isDebug() {
        return this.debug;
    }

    public boolean injectCommands() {
        return this.injectCommands;
    }

    public boolean enabledResourcePacks() {
        return this.enableResourcePacks;
    }

    public void setForcePacks(boolean forcePacks) {
        this.forcePacks = forcePacks;
    }

    public boolean forcePacks() {
        return this.forcePacks;
    }

    public void setPackCacheSize(int packCacheSize) {
        this.packCacheSize = packCacheSize;
    }

    public int getPackCacheSize() {
        return this.packCacheSize;
    }
}
