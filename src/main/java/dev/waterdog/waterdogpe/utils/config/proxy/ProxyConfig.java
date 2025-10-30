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

package dev.waterdog.waterdogpe.utils.config.proxy;

import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.network.connection.codec.compression.CompressionType;
import dev.waterdog.waterdogpe.utils.config.ServerList;
import dev.waterdog.waterdogpe.utils.config.serializer.CompressionAlgorithmConverter;
import dev.waterdog.waterdogpe.utils.config.serializer.InetSocketAddressConverter;
import dev.waterdog.waterdogpe.utils.config.serializer.ServerEntryConverter;
import dev.waterdog.waterdogpe.utils.config.serializer.ServerListConverter;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.cubespace.Yamler.Config.YamlConfig;
import net.cubespace.Yamler.Config.*;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.*;

@Getter @Setter
@SerializeOptions(skipFailedObjects = true)
public class ProxyConfig extends YamlConfig {

    @Path("listener.motd")
    @Comment("The Motd which will be displayed in the server tab of a player and returned during ping")
    private String motd = "§bWaterdog§3PE";

    @Path("listener.name")
    @Comment("The name that is shown up in the player list (pause menu)")
    private String name = "§bWaterdog§3PE";

    @Path("listener.priorities")
    @Comment("The server priority list. If not changed by plugins, the proxy will connect the player to the first of those servers")
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

    @Path("servers")
    @Comments({
            "A list of all downstream servers that are available right after starting",
            "address field is formatted using ip:port",
            "publicAddress is optional and can be set to the ip players can directly connect through"
    })
    private ServerList serverList = new ServerList().initEmpty();

    @Path("network_settings")
    @Comment("Connection and security related settings. Do NOT edit unless you know what you are doing!")
    private NetworkSettings networkSettings = new NetworkSettings();

    @Path("permissions")
    @Comment("Case-Sensitive permission list for players (empty using {})")
    private Object2ObjectOpenHashMap<String, List<String>> playerPermissions = new Object2ObjectOpenHashMap<>() {{
        this.put("alemiz003", Arrays.asList("waterdog.player.transfer", "waterdog.player.list"));
        this.put("TobiasDev", Arrays.asList("waterdog.player.transfer", "waterdog.player.list"));
    }};

    @Path("permissions_default")
    @Comment("List of permissions each player should get by default (empty using [])")
    private List<String> defaultPermissions = new ArrayList<>(Arrays.asList("waterdog.command.help", "waterdog.command.info"));

    @Path("enable_debug")
    @Comment("Whether the debug output in the console should be enabled or not")
    private boolean debug;

    @Path("upstream_encryption")
    @Comment("If enabled, encrypted connection between client and proxy will be created")
    private boolean upstreamEncryption = true;

    @Path("online_mode")
    @Comment("If enabled, only players which are authenticated with XBOX Live can join. If disabled, anyone can connect *with any name*")
    private boolean onlineMode = true;

    @Path("listener.additional_ports")
    @Comment("Additional ports to listen to")
    private List<Integer> additionalPorts = new ArrayList<>();

    @Path("listener.join_handler")
    @Comment("The join handler used to determine the first entered server. Possible options: DefaultJoinHandler")
    private String joinHandler = "DefaultJoinHandler";

    @Path("listener.reconnect_handler")
    @Comment("The reconnect handler used to determine fallback connections. Possible options: DefaultReconnectHandler, RoundRobinReconnectHandler")
    private String reconnectHandler = "DefaultReconnectHandler";

    @Path("use_login_extras")
    @Accessors(fluent = true)
    @Comment("If enabled, the proxy will pass information like XUID or IP to the downstream server using custom fields in the LoginPacket")
    private boolean useLoginExtras = false;

    @Path("use_certificate_payload")
    @Accessors(fluent = true)
    @Comment("If enabled, the proxy will always send Certificate payload in the LoginPacket")
    private boolean useCertificatePayload = true;

    @Path("replace_username_spaces")
    @Comment("Replaces username spaces with underscores if enabled")
    private boolean replaceUsernameSpaces = false;

    @Path("enable_query")
    @Accessors(fluent = true)
    @Comment("Whether server query should be enabled")
    private boolean enableQuery = true;

    @Path("prefer_fast_transfer")
    @Accessors(fluent = true)
    @Comment("If enabled, when receiving a McpeTransferPacket, the proxy will check if the target server is in the downstream list, and if yes, use the fast transfer mechanism")
    private boolean useFastTransfer = true;

    @Path("inject_proxy_commands")
    @Accessors(fluent = true)
    @Comment("If enabled, the proxy will inject all the proxy commands in the AvailableCommandsPacket, enabling autocompletion")
    private boolean injectCommands = true;

    @Path("compression")
    @Comments({
            "Algorithm used for upstream compression. Currently supported: zlib, snappy",
            "This is only applicable on 1.19.30 and newer versions"
    })
    private CompressionType compression = CompressionType.ZLIB;

    @Path("upstream_compression_level")
    @Comment("Upstream server compression ratio(proxy to client), higher = less bandwidth, more cpu, lower vice versa")
    private int upstreamCompression = 6;

    @Path("downstream_compression_level")
    @Comment("Downstream server compression ratio(proxy to downstream server), higher = less bandwidth, more cpu, lower vice versa")
    private int downstreamCompression = 2;

    @Path("enable_edu_features")
    @Accessors(fluent = true)
    @Comment("Education features require small adjustments to work correctly. Enable this option if any of downstream servers support education features.")
    private boolean enableEducationFeatures = true;

    @Path("enable_packs")
    @Accessors(fluent = true)
    @Comment("Enable/Disable the resource pack system")
    private boolean enableResourcePacks = true;

    @Path("overwrite_client_packs")
    @Comment("If this is enabled, the client will not be able to use custom packs")
    private boolean overwriteClientPacks = false;

    @Path("force_server_packs")
    @Comment("If enabled, the client will be forced to accept server-sided resource packs")
    private boolean forceServerPacks = false;

    @Path("pack_cache_size")
    @Comment("You can set maximum pack size in MB to be cached.")
    private int packCacheSize = 16;

    @Path("default_idle_threads")
    @Comment("Creating threads may be in some situations expensive. Specify minimum count of idle threads per internal thread executors. Set to -1 to auto-detect by core count.")
    private int defaultIdleThreads = -1;

    @Path("enable_statistics")
    @Comment("Enable anonymous statistics that are sent to bstats. For more information, check out our bstats page at https://bstats.org/plugin/server-implementation/WaterdogPE/15678")
    private boolean enableAnonymousStatistics = true;

    @Path("enable_error_reporting")
    @Comment("Enables anonymous error reporting using bugsnag. This allows the WaterdogPE team to automatically collect issues occurring on WaterdogPE instances.")
    private boolean enableAnonymousErrorReporting = true;

    public ProxyConfig(File file) {
        this.CONFIG_HEADER = new String[]{"Waterdog Main Configuration file", "Configure your desired network settings here."};
        this.CONFIG_FILE = file;
        try {
            this.addConverter(InetSocketAddressConverter.class);
            this.addConverter(ServerEntryConverter.class);
            this.addConverter(ServerListConverter.class);
            this.addConverter(CompressionAlgorithmConverter.class);
        } catch (InvalidConverterException e) {
            ProxyServer.getInstance().getLogger().error("Error while initiating config converters", e);
        }
    }

    public int getIdleThreads() {
        return this.defaultIdleThreads < 1 ? Runtime.getRuntime().availableProcessors() : this.defaultIdleThreads;
    }
}
