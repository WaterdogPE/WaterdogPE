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

package dev.waterdog.waterdogpe;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.nukkitx.network.util.EventLoops;
import com.nukkitx.network.util.NetworkThreadFactory;
import com.nukkitx.protocol.bedrock.BedrockClient;
import com.nukkitx.protocol.bedrock.BedrockServer;
import dev.waterdog.waterdogpe.command.*;
import dev.waterdog.waterdogpe.command.utils.CommandUtils;
import dev.waterdog.waterdogpe.console.TerminalConsole;
import dev.waterdog.waterdogpe.event.EventManager;
import dev.waterdog.waterdogpe.event.defaults.DispatchCommandEvent;
import dev.waterdog.waterdogpe.event.defaults.ProxyStartEvent;
import dev.waterdog.waterdogpe.logger.MainLogger;
import dev.waterdog.waterdogpe.network.ProxyListener;
import dev.waterdog.waterdogpe.network.protocol.ProtocolConstants;
import dev.waterdog.waterdogpe.network.protocol.ProtocolVersion;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfoMap;
import dev.waterdog.waterdogpe.network.session.CompressionAlgorithm;
import dev.waterdog.waterdogpe.packs.PackManager;
import dev.waterdog.waterdogpe.player.PlayerManager;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import dev.waterdog.waterdogpe.plugin.PluginManager;
import dev.waterdog.waterdogpe.query.QueryHandler;
import dev.waterdog.waterdogpe.scheduler.WaterdogScheduler;
import dev.waterdog.waterdogpe.utils.ConfigurationManager;
import dev.waterdog.waterdogpe.utils.bstats.Metrics;
import dev.waterdog.waterdogpe.utils.config.LangConfig;
import dev.waterdog.waterdogpe.utils.config.ProxyConfig;
import dev.waterdog.waterdogpe.utils.types.*;
import io.netty.channel.EventLoopGroup;
import net.cubespace.Yamler.Config.InvalidConfigurationException;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;

public class ProxyServer {

    private static ProxyServer instance;

    private final Path dataPath;
    private final Path pluginPath;
    private final Path packsPath;

    private final MainLogger logger;
    private final TerminalConsole console;

    private final ConfigurationManager configurationManager;
    private final WaterdogScheduler scheduler;
    private final PlayerManager playerManager;
    private final PluginManager pluginManager;
    private final EventManager eventManager;
    private final PackManager packManager;

    private final ServerInfoMap serverInfoMap = new ServerInfoMap();

    private BedrockServer bedrockServer;
    private QueryHandler queryHandler;

    private CommandMap commandMap;
    private final ConsoleCommandSender commandSender;

    private IReconnectHandler reconnectHandler;
    private IJoinHandler joinHandler;
    private IForcedHostHandler forcedHostHandler;
    private IMetricsHandler metricsHandler;
    private ProxyListenerInterface proxyListener = new ProxyListenerInterface() {
    };

    private final EventLoopGroup bossEventLoopGroup;
    private final EventLoopGroup workerEventLoopGroup;
    private final ScheduledExecutorService tickExecutor;
    private ScheduledFuture<?> tickFuture;
    private boolean shutdown = false;
    private int currentTick = 0;
    private Metrics metrics;

    public ProxyServer(MainLogger logger, String filePath, String pluginPath) throws InvalidConfigurationException {
        instance = this;
        this.logger = logger;
        this.dataPath = Paths.get(filePath);
        this.pluginPath = Paths.get(pluginPath);
        this.packsPath = this.dataPath.resolve("packs");

        if (!this.pluginPath.toFile().exists()) {
            if (this.pluginPath.toFile().mkdirs())
                this.logger.info("Created Plugin Folder at " + this.pluginPath);
            else
                this.logger.warning("Could not create Plugin Folder at " + this.pluginPath);
        }

        if (!this.packsPath.toFile().exists()) {
            if (this.packsPath.toFile().mkdirs())
                this.logger.info("Created Packs Folder at " + this.packsPath);
            else
                this.logger.warning("Could not create Packs Folder at " + this.packsPath);
        }

        this.configurationManager = new ConfigurationManager(this);
        this.configurationManager.loadProxyConfig();
        this.configurationManager.loadLanguage();

        if (!this.getConfiguration().isIpv6Enabled()) {
            // Some devices and networks may not support IPv6
            System.setProperty("java.net.preferIPv4Stack", "true");
        }

        if (this.getConfiguration().isDebug()) {
            WaterdogPE.version().debug(true);
        }

        CompressionAlgorithm compression = this.getConfiguration().getCompression();
        if (compression.getBedrockCompression() == null) {
            this.logger.error("Bedrock compression supports only ZLIB or Snappy! Currently provided " + compression + ", defaulting to ZLIB!");
            this.getConfiguration().setCompression(CompressionAlgorithm.ZLIB);
        }

        ThreadFactoryBuilder builder = new ThreadFactoryBuilder();
        builder.setNameFormat("WaterdogTick Executor");
        this.tickExecutor = Executors.newScheduledThreadPool(1, builder.build());

        EventLoops.ChannelType channelType = EventLoops.getChannelType();
        this.logger.info("Using " + channelType.name() + " channel implementation as default!");
        for (EventLoops.ChannelType type : EventLoops.ChannelType.values()) {
            this.logger.debug("Supported " + type.name() + " channels: " + type.isAvailable());
        }

        NetworkThreadFactory workerFactory = NetworkThreadFactory.builder()
                .format("Bedrock Listener - #%d")
                .priority(5)
                .daemon(true)
                .build();
        NetworkThreadFactory bossFactory = NetworkThreadFactory.builder()
                .format("RakNet Listener - #%d")
                .priority(8)
                .daemon(true)
                .build();
        this.workerEventLoopGroup = channelType.newEventLoopGroup(0, workerFactory);
        this.bossEventLoopGroup = channelType.newEventLoopGroup(0, bossFactory);

        // Default Handlers
        this.reconnectHandler = new VanillaReconnectHandler();
        this.forcedHostHandler = new VanillaForcedHostHandler();
        this.metricsHandler = new VanillaMetricsHandler();
        this.joinHandler = new VanillaJoinHandler(this);
        this.pluginManager = new PluginManager(this);
        this.configurationManager.loadServerInfos(this.serverInfoMap);
        this.scheduler = new WaterdogScheduler(this);
        this.playerManager = new PlayerManager(this);
        this.eventManager = new EventManager(this);
        this.packManager = new PackManager(this);

        this.commandSender = new ConsoleCommandSender(this);
        this.commandMap = new DefaultCommandMap(this, SimpleCommandMap.DEFAULT_PREFIX);
        this.console = new TerminalConsole(this);
        this.boot();
    }

    public static ProxyServer getInstance() {
        return instance;
    }

    private void boot() {
        this.console.getConsoleThread().start();
        this.pluginManager.enableAllPlugins();
        if (this.getConfiguration().useFastCodec()) {
            this.logger.debug("Using fast codec! Please ensure plugin compatibility!");
            ProtocolConstants.registerCodecs();
        }

        if(this.getConfiguration().isEnableAnonymousStatistics()){
            Metrics.WaterdogMetrics.startMetrics(this, this.getConfiguration());
            this.getLogger().info("Enabling anonymous statistics.");
        }

        if (this.getConfiguration().enabledResourcePacks()) {
            this.packManager.loadPacks(this.packsPath);
        }

        InetSocketAddress bindAddress = this.getConfiguration().getBindAddress();
        this.logger.info("Binding to " + bindAddress);

        if (this.getConfiguration().isEnabledQuery()) {
            this.queryHandler = new QueryHandler(this, bindAddress);
        }

        this.bedrockServer = new BedrockServer(bindAddress, Runtime.getRuntime().availableProcessors(), this.bossEventLoopGroup, this.workerEventLoopGroup, false);
        this.bedrockServer.setHandler(new ProxyListener(this));
        this.bedrockServer.bind().join();

        ProxyStartEvent event = new ProxyStartEvent(this);
        this.eventManager.callEvent(event);

        this.logger.debug("Upstream <-> Proxy compression level " + this.getConfiguration().getUpstreamCompression());
        this.logger.debug("Downstream <-> Proxy compression level " + this.getConfiguration().getDownstreamCompression());

        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
        this.tickFuture = this.tickExecutor.scheduleAtFixedRate(this::tickProcessor, 50, 50, TimeUnit.MILLISECONDS);
    }

    private void tickProcessor() {
        if (this.shutdown && !this.tickFuture.isCancelled()) {
            this.tickFuture.cancel(false);
            this.bedrockServer.close();
        }

        try {
            this.onTick(++this.currentTick);
        } catch (Exception e) {
            this.logger.error("Error while ticking proxy!", e);
        }
    }

    private void onTick(int currentTick) {
        this.scheduler.onTick(currentTick);
    }

    public void shutdown() {
        if (this.shutdown) {
            return;
        }
        this.shutdown = true;

        try {
            this.shutdown0();
        } catch (Exception e) {
            this.logger.error("Unable to shutdown proxy gracefully", e);
        } finally {
            WaterdogPE.shutdownHook();
        }
    }

    private void shutdown0() throws Exception {
        this.pluginManager.disableAllPlugins();
        String disconnectReason = new TranslationContainer("waterdog.server.shutdown").getTranslated();
        for (Map.Entry<UUID, ProxiedPlayer> player : this.playerManager.getPlayers().entrySet()) {
            this.logger.info("Disconnecting " + player.getValue().getName());
            player.getValue().disconnect(disconnectReason, true);
        }
        Thread.sleep(500); // Give small delay to send packet

        this.console.getConsoleThread().interrupt();
        this.tickExecutor.shutdown();
        this.scheduler.shutdown();
        this.eventManager.getThreadedExecutor().shutdown();
        try {
            if (this.bedrockServer != null) {
                this.bedrockServer.close();
            }
        } catch (Exception e) {
            this.getLogger().error("Error while shutting down ProxyServer", e);
        }

        if (!this.tickFuture.isCancelled()) {
            this.logger.info("Interrupting scheduler!");
            this.tickFuture.cancel(true);
        }
        this.logger.info("Shutdown complete!");
    }

    public String translate(TextContainer textContainer) {
        return this.getLanguageConfig().translateContainer(textContainer);
    }

    public boolean handlePlayerCommand(ProxiedPlayer player, String message) {
        if (!this.commandMap.handleMessage(player, message)) {
            return false;
        }
        return this.dispatchCommand(player, message.substring(this.commandMap.getCommandPrefix().length()));
    }

    public boolean dispatchCommand(CommandSender sender, String message) {
        if (message.trim().isEmpty()) {
            return false;
        }

        String[] args = message.split(" ");
        if (args.length < 1) {
            return false;
        }

        Command command = this.getCommandMap().getCommand(args[0]);
        if (command == null)  {
            return false;
        }

        String[] shiftedArgs;
        if (command.getSettings().isQuoteAware()) { // Quote aware parsing
            List<String> arguments = CommandUtils.parseArguments(message);
            arguments.remove(0);
            shiftedArgs = arguments.toArray(String[]::new);
        } else {
            shiftedArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];
        }

        DispatchCommandEvent event = new DispatchCommandEvent(sender, args[0], shiftedArgs);
        this.eventManager.callEvent(event);
        return !event.isCancelled() && this.commandMap.handleCommand(sender, args[0], shiftedArgs);
    }

    public BedrockClient createBedrockClient() {
        InetSocketAddress address = new InetSocketAddress("0.0.0.0", 0);
        return new BedrockClient(address, this.bossEventLoopGroup);
    }

    public CompletableFuture<BedrockClient> bindClient(ProtocolVersion protocol) {
        BedrockClient client = this.createBedrockClient();
        client.setRakNetVersion(protocol.getRaknetVersion());
        return client.bind().thenApply(i -> client);
    }

    public boolean isRunning() {
        return !this.shutdown;
    }

    public MainLogger getLogger() {
        return this.logger;
    }

    public BedrockServer getBedrockServer() {
        return this.bedrockServer;
    }

    public Path getDataPath() {
        return this.dataPath;
    }

    public ConfigurationManager getConfigurationManager() {
        return this.configurationManager;
    }

    public ProxyConfig getConfiguration() {
        return this.configurationManager.getProxyConfig();
    }

    public LangConfig getLanguageConfig() {
        return this.configurationManager.getLangConfig();
    }

    public WaterdogScheduler getScheduler() {
        return this.scheduler;
    }

    public PlayerManager getPlayerManager() {
        return this.playerManager;
    }

    public ProxiedPlayer getPlayer(UUID uuid) {
        return this.playerManager.getPlayer(uuid);
    }

    public ProxiedPlayer getPlayer(String playerName) {
        return this.playerManager.getPlayer(playerName);
    }

    public Map<UUID, ProxiedPlayer> getPlayers() {
        return this.playerManager.getPlayers();
    }

    @Deprecated
    public ServerInfo getServer(String serverName) {
        return this.serverInfoMap.get(serverName.toLowerCase());
    }

    /**
     * Allows to add servers dynamically to server map
     *
     * @return if server was registered
     */
    public boolean registerServerInfo(ServerInfo serverInfo) {
        Preconditions.checkNotNull(serverInfo, "ServerInfo can not be null!");
        return this.serverInfoMap.putIfAbsent(serverInfo.getServerName(), serverInfo) == null;
    }

    /**
     * Remove server from server map
     *
     * @return removed ServerInfo or null
     */
    public ServerInfo removeServerInfo(String serverName) {
        Preconditions.checkNotNull(serverName, "ServerName can not be null!");
        return this.serverInfoMap.remove(serverName);
    }

    public ServerInfo getServerInfo(String serverName) {
        Preconditions.checkNotNull(serverName, "ServerName can not be null!");
        return this.serverInfoMap.get(serverName);
    }

    /**
     * Get ServerInfo by address and port
     *
     * @return ServerInfo instance of matched server
     */
    public ServerInfo getServerInfo(String address, int port) {
        Preconditions.checkNotNull(address, "Address can not be null!");
        for (ServerInfo serverInfo : this.getServers()) {
            if (serverInfo.matchAddress(address, port)) {
                return serverInfo;
            }
        }
        return null;
    }

    /**
     * Get ServerInfo instance using hostname
     *
     * @return ServerInfo assigned to forced host
     */
    public ServerInfo getForcedHost(String serverHostname) {
        Preconditions.checkNotNull(serverHostname, "ServerHostname can not be null!");
        String serverName = null;

        for (String forcedHost : this.getConfiguration().getForcedHosts().keySet()) {
            if (forcedHost.equalsIgnoreCase(serverHostname)) {
                serverName = this.getConfiguration().getForcedHosts().get(forcedHost);
                break;
            }
        }
        return serverName == null ? null : this.serverInfoMap.get(serverName);
    }

    /**
     * Get all registered ServerInfo instances
     *
     * @return an unmodifiable collection containing all registered ServerInfo instances
     */
    public Collection<ServerInfo> getServers() {
        return this.serverInfoMap.values();
    }

    public ServerInfoMap getServerInfoMap() {
        return this.serverInfoMap;
    }

    public Path getPluginPath() {
        return this.pluginPath;
    }

    public PluginManager getPluginManager() {
        return this.pluginManager;
    }

    public int getCurrentTick() {
        return this.currentTick;
    }

    public EventManager getEventManager() {
        return this.eventManager;
    }

    public PackManager getPackManager() {
        return this.packManager;
    }

    public QueryHandler getQueryHandler() {
        return this.queryHandler;
    }

    public CommandMap getCommandMap() {
        return this.commandMap;
    }

    public void setCommandMap(CommandMap commandMap) {
        Preconditions.checkNotNull(commandMap, "Command map can not be null!");
        this.commandMap = commandMap;
    }

    public ConsoleCommandSender getConsoleSender() {
        return this.commandSender;
    }

    public IJoinHandler getJoinHandler() {
        return this.joinHandler;
    }

    public void setJoinHandler(IJoinHandler joinHandler) {
        this.joinHandler = joinHandler;
    }

    public IReconnectHandler getReconnectHandler() {
        return this.reconnectHandler;
    }

    public IForcedHostHandler getForcedHostHandler() {
        return forcedHostHandler;
    }

    public void setForcedHostHandler(IForcedHostHandler forcedHostHandler) {
        this.forcedHostHandler = forcedHostHandler;
    }

    public IMetricsHandler getMetricsHandler() {
        return metricsHandler;
    }

    public void setMetricsHandler(IMetricsHandler metricsHandler) {
        Preconditions.checkNotNull(metricsHandler, "You cannot set the metricsHandler to null!");
        this.metricsHandler = metricsHandler;
    }

    public void setReconnectHandler(IReconnectHandler reconnectHandler) {
        this.reconnectHandler = reconnectHandler;
    }

    @Deprecated
    public boolean isDebug() {
        return WaterdogPE.version().debug();
    }

    public void setProxyListener(ProxyListenerInterface proxyListener) {
        Preconditions.checkNotNull(proxyListener, "Proxy listener can not be null!");
        this.proxyListener = proxyListener;
    }

    public ProxyListenerInterface getProxyListener() {
        return this.proxyListener;
    }
}
