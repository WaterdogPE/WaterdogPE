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
import net.cubespace.Yamler.Config.YamlConfig;
import net.cubespace.Yamler.Config.*;
import pe.waterdog.ProxyServer;
import pe.waterdog.utils.config.InetSocketAddressConverter;
import pe.waterdog.utils.config.ServerInfoConverter;
import pe.waterdog.utils.config.ServerList;
import pe.waterdog.utils.config.ServerListConverter;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.*;

public class ProxyConfig extends YamlConfig {

    @Path("servers")
    @Comments({
            "A list of all downstream proxies that are available right after starting",
            "address field is formatted using ip:port",
            "publicAddress is optional and can be set to the ip players can directly connect through"
    })
    private ServerList serverInfoMap = new ServerList().initEmpty();

    @Path("listener.motd")
    @Comment("The Motd which will be displayed in the server tab of a player and returned during ping")
    private String motd = "§bWaterdog§3PE";

    @Path("listener.priorities")
    @Comment("The server priority list. if not changed by plugins, the proxy will connect the player to the first of those servers")
    private List<String> priorities = new ArrayList<>(Collections.singletonList("lobby1"));

    @Path("listener.host")
    @Comment("The address to bind the server to")
    private InetSocketAddress bindAddress = new InetSocketAddress("0.0.0.0", 19132);

    @Path("listener.max_players")
    @Comment("The maximum amount of players that can connect to this proxy instance")
    private int maxPlayerCount = 20;

    @Path("listener.forced_hosts")
    @Comments({
            "Map the ip a player joined through to a specific server",
            "for example skywars.xyz.com => SkyWars-1",
            "when a player connects using skywars-xyz.com as the serverIp, he will be connected to SkyWars-1 directly"
    })
    private Map<String, String> forcedHosts = new HashMap<>();

    @Path("permissions")
    @Comment("Case-Sensitive permission list for players")
    private Object2ObjectOpenHashMap<String, List<String>> playerPermissions = new Object2ObjectOpenHashMap<>() {{
        this.put("alemiz003", Arrays.asList("waterdog.player.transfer", "waterdog.player.list"));
        this.put("TobiasDev", Arrays.asList("waterdog.player.transfer", "waterdog.player.list"));
    }};

    @Path("permissions_default")
    @Comment("List of permissions each player should get by default")
    private List<String> defaultPermissions = new ArrayList<>(Arrays.asList("waterdog.command.help", "waterdog.command.info"));

    @Path("enable_debug")
    @Comment("Whether the debug output in the console should be enabled or not")
    private boolean debug;

    @Path("online_mode")
    @Comment("If enabled, only players which are authenticated with XBOX Live can join. If disabled, anyone can connect *with any name*")
    private boolean onlineMode = true;

    @Path("use_login_extras")
    @Comment("If enabled, the proxy will pass information like XUID or IP to the downstream server using custom fields in the LoginPacket")
    private boolean useLoginExtras = true;

    @Path("ip_forward")
    @Comment("Forward original address in client data under 'Waterdog_IP' attribute.")
    private boolean ipForward = false;

    @Path("replace_username_spaces")
    @Comment("Replaces username spaces with underscores if enabled")
    private boolean replaceUsernameSpaces = false;

    @Path("enable_query")
    @Comment("Whether server query should be enabled")
    private boolean enableQuery = true;

    @Path("prefer_fast_transfer")
    @Comment("If enabled, when receiving a McpeTransferPacket, the proxy will check if the target server is in the downstream list, and if yes, use the fast transfer mechanism")
    private boolean fastTransfer = true;

    @Path("use_fast_codec")
    @Comment("Fast-codec only decodes the packets required by the proxy, everything else will be passed rawly. Disabling this can create a performance hit")
    private boolean fastCodec = true;

    @Path("inject_proxy_commands")
    @Comment("If enabled, the proxy will inject all the proxy commands in the AvailableCommandsPacket, enabling autocompletion")
    private boolean injectCommands = true;

    @Path("upstream_compression_level")
    @Comment("Upstream server compression ratio(proxy to client), higher = less bandwidth, more cpu, lower vice versa")
    private int upstreamCompression = 6;

    @Path("downstream_compression_level")
    @Comment("Upstream server compression ratio(proxy to downstream server), higher = less bandwidth, more cpu, lower vice versa")
    private int downstreamCompression = 2;

    @Path("enable_packs")
    @Comment("Enable/Disable the resource pack system")
    private boolean enableResourcePacks = true;

    @Path("force_apply_packs")
    @Comment("Whether texture packs should be enforced")
    private boolean forcePacks = false;

    @Path("pack_cache_size")
    @Comment("You can set maximum pack size in MB to be cached.")
    private int packCacheSize = 16;

    public ProxyConfig(File file) {
        this.CONFIG_HEADER = new String[]{"Waterdog Main Configuration file", "Configure your desired network settings here."};
        this.CONFIG_FILE = file;
        try {
            this.addConverter(InetSocketAddressConverter.class);
            this.addConverter(ServerInfoConverter.class);
            this.addConverter(ServerListConverter.class);
        } catch (InvalidConverterException e) {
            ProxyServer.getInstance().getLogger().error("Error while initiating config converters", e);
        }
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

    public ServerList getServerInfoMap() {
        return this.serverInfoMap;
    }
}
