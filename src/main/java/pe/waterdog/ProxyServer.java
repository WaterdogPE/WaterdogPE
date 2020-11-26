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

package pe.waterdog;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.nukkitx.protocol.bedrock.BedrockClient;
import com.nukkitx.protocol.bedrock.BedrockServer;
import lombok.SneakyThrows;
import net.cubespace.Yamler.Config.InvalidConfigurationException;
import org.apache.logging.log4j.Level;
import pe.waterdog.command.*;
import pe.waterdog.console.TerminalConsole;
import pe.waterdog.event.EventManager;
import pe.waterdog.event.defaults.DispatchCommandEvent;
import pe.waterdog.logger.MainLogger;
import pe.waterdog.network.ProxyListener;
import pe.waterdog.network.ServerInfo;
import pe.waterdog.network.protocol.ProtocolConstants;
import pe.waterdog.network.protocol.ProtocolVersion;
import pe.waterdog.packs.PackManager;
import pe.waterdog.player.PlayerManager;
import pe.waterdog.player.ProxiedPlayer;
import pe.waterdog.plugin.PluginManager;
import pe.waterdog.query.QueryHandler;
import pe.waterdog.scheduler.WaterdogScheduler;
import pe.waterdog.utils.ConfigurationManager;
import pe.waterdog.utils.LangConfig;
import pe.waterdog.utils.ProxyConfig;
import pe.waterdog.utils.config.ServerList;
import pe.waterdog.utils.types.*;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
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

    private BedrockServer bedrockServer;
    private final ServerList serverInfoMap;
    private QueryHandler queryHandler;

    private CommandMap commandMap;
    private final ConsoleCommandSender commandSender;

    private IReconnectHandler reconnectHandler;
    private IJoinHandler joinHandler;

    private final ScheduledExecutorService tickExecutor;
    private ScheduledFuture<?> tickFuture;
    private boolean shutdown = false;
    private int currentTick = 0;

    public ProxyServer(MainLogger logger, String filePath, String pluginPath) throws InvalidConfigurationException {
        instance = this;
        this.logger = logger;
        this.dataPath = Paths.get(filePath);
        this.pluginPath = Paths.get(pluginPath);
        this.packsPath = this.dataPath.resolve("packs");

        if (!new File(pluginPath).exists()) {
            this.logger.info("Created Plugin Folder at " + this.pluginPath.toString());
            new File(pluginPath).mkdirs();
        }

        if (!this.packsPath.toFile().exists()) {
            this.logger.info("Created Packs Folder at " + this.packsPath.toString());
            this.packsPath.toFile().mkdirs();
        }

        ThreadFactoryBuilder builder = new ThreadFactoryBuilder();
        builder.setNameFormat("WaterdogTick Executor");
        this.tickExecutor = Executors.newScheduledThreadPool(1, builder.build());

        this.configurationManager = new ConfigurationManager(this);
        configurationManager.loadProxyConfig();
        if (this.getConfiguration().isDebug()) {
            WaterdogPE.setLoggerLevel(Level.DEBUG);
        }
        configurationManager.loadLanguage();
        // Default Handlers
        this.reconnectHandler = new VanillaReconnectHandler();
        this.joinHandler = new VanillaJoinHandler(this);
        this.serverInfoMap = configurationManager.getProxyConfig().getServerInfoMap();
        this.pluginManager = new PluginManager(this);
        this.scheduler = new WaterdogScheduler(this);
        this.playerManager = new PlayerManager(this);
        this.eventManager = new EventManager();
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

        if (this.getConfiguration().enabledResourcePacks()) {
            this.packManager.loadPacks(this.packsPath);
        }

        InetSocketAddress bindAddress = this.getConfiguration().getBindAddress();
        this.logger.info("Binding to " + bindAddress);

        if (this.getConfiguration().isEnabledQuery()) {
            this.queryHandler = new QueryHandler(this, bindAddress);
        }

        this.bedrockServer = new BedrockServer(bindAddress, Runtime.getRuntime().availableProcessors());
        bedrockServer.setHandler(new ProxyListener(this));
        bedrockServer.bind().join();

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

    @SneakyThrows
    public void shutdown() {
        if (this.shutdown) {
            return;
        }
        this.shutdown = true;
        this.pluginManager.disableAllPlugins();

        for (Map.Entry<UUID, ProxiedPlayer> player : this.playerManager.getPlayers().entrySet()) {
            this.logger.info("Disconnecting " + player.getValue().getName());
            player.getValue().disconnect("Proxy Shutdown");
        }
        Thread.sleep(500); // Give small delay to send packet

        this.console.getConsoleThread().interrupt();
        this.tickExecutor.shutdown();
        this.scheduler.shutdown();
        this.eventManager.getThreadedExecutor().shutdown();
        try {
            if (this.bedrockServer != null) {
                this.bedrockServer.getRakNet().close();
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
        DispatchCommandEvent event = new DispatchCommandEvent(sender, message);
        this.eventManager.callEvent(event);

        if (event.isCancelled()) {
            return false;
        }
        String[] args = message.split(" ");
        return this.commandMap.handleCommand(sender, args[0], Arrays.copyOfRange(args, 1, args.length));
    }

    public CompletableFuture<BedrockClient> bindClient(ProtocolVersion protocol) {
        int port;
        try {
            ServerSocket socket = new ServerSocket(0);
            socket.setReuseAddress(true);
            port = socket.getLocalPort();
        }catch (IOException e){
            throw new RuntimeException("Can bind BedrockClient!", e);
        }
        InetSocketAddress address = new InetSocketAddress("0.0.0.0", port);
        BedrockClient client = new BedrockClient(address);
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
        String serverName = this.getConfiguration().getForcedHosts().get(serverHostname);
        return serverName == null ? null : this.serverInfoMap.get(serverName);
    }

    public Collection<ServerInfo> getServers() {
        return new HashSet<>(this.serverInfoMap.values());
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

    public void setReconnectHandler(IReconnectHandler reconnectHandler) {
        this.reconnectHandler = reconnectHandler;
    }

    public boolean isDebug() {
        return VersionInfo.IS_DEVELOPMENT || this.getConfiguration().isDebug();
    }
}
