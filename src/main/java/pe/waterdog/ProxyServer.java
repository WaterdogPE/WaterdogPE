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
import com.nukkitx.protocol.bedrock.BedrockServer;
import lombok.SneakyThrows;
import pe.waterdog.command.*;
import pe.waterdog.console.TerminalConsole;
import pe.waterdog.event.EventManager;
import pe.waterdog.event.defaults.DispatchCommandEvent;
import pe.waterdog.logger.MainLogger;
import pe.waterdog.network.ProxyListener;
import pe.waterdog.network.ServerInfo;
import pe.waterdog.player.PlayerManager;
import pe.waterdog.player.ProxiedPlayer;
import pe.waterdog.plugin.PluginManager;
import pe.waterdog.query.QueryHandler;
import pe.waterdog.scheduler.WaterdogScheduler;
import pe.waterdog.utils.ConfigurationManager;
import pe.waterdog.utils.LangConfig;
import pe.waterdog.utils.ProxyConfig;
import pe.waterdog.utils.types.TextContainer;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

public class ProxyServer {

    private static ProxyServer instance;

    private Path dataPath;
    private Path pluginPath;

    private final MainLogger logger;
    private final TerminalConsole console;

    private BedrockServer bedrockServer;
    private QueryHandler queryHandler;

    private ConfigurationManager configurationManager;
    private WaterdogScheduler scheduler;
    private final PlayerManager playerManager;
    private final PluginManager pluginManager;
    private final EventManager eventManager;
    private boolean shutdown = false;

    private Map<String, ServerInfo> serverInfoMap;

    private final ConsoleCommandSender commandSender;
    private CommandMap commandMap;

    private int currentTick = 0;
    private long nextTick;

    public ProxyServer(MainLogger logger, String filePath, String pluginPath) {
        instance = this;
        this.logger = logger;
        this.dataPath = Paths.get(filePath);
        this.pluginPath = Paths.get(pluginPath);

        if (!new File(pluginPath).exists()) {
            this.logger.info("Created Plugin Folder at " + this.pluginPath.toString());
            new File(pluginPath).mkdirs();
        }

        this.configurationManager = new ConfigurationManager(this);
        configurationManager.loadProxyConfig();
        configurationManager.loadLanguage();

        this.serverInfoMap = configurationManager.getProxyConfig().buildServerMap();

        this.pluginManager = new PluginManager(this);
        this.scheduler = new WaterdogScheduler(this);
        this.playerManager = new PlayerManager(this);
        this.eventManager = new EventManager();

        this.commandSender = new ConsoleCommandSender(this);
        this.commandMap = new DefaultCommandMap(this, SimpleCommandMap.DEFAULT_PREFIX);
        this.console = new TerminalConsole(this);
        this.boot();
        this.tickProcessor();
    }

    public static ProxyServer getInstance() {
        return instance;
    }

    private void boot() {
        this.console.getConsoleThread().start();
        this.pluginManager.enableAllPlugins();

        InetSocketAddress bindAddress = this.getConfiguration().getBindAddress();
        this.logger.info("Binding to " + bindAddress);

        if (this.getConfiguration().isEnabledQuery()){
            this.queryHandler = new QueryHandler(this, bindAddress);
        }

        this.bedrockServer = new BedrockServer(bindAddress, Runtime.getRuntime().availableProcessors());
        bedrockServer.setHandler(new ProxyListener(this));
        bedrockServer.bind().join();
    }

    private void tickProcessor() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
        this.nextTick = System.currentTimeMillis();

        while (!this.shutdown) {
            long tickTime = System.currentTimeMillis();
            long delay = this.nextTick - tickTime;

            if (delay >= 50){
                try {
                    Thread.sleep(Math.max(25, delay - 25));
                } catch (InterruptedException e) {
                    this.logger.error("Main thread interrupted whilst sleeping", e);
                }
            }

            try {
                this.onTick(++this.currentTick);
            }catch (Exception e){
                this.logger.error("Error while ticking proxy!", e);
            }

            if (this.nextTick - tickTime < -1000){
                //We are not doing 20 ticks per second
                this.nextTick = tickTime;
            }else {
                this.nextTick += 50;
            }
        }

        this.bedrockServer.close();
    }

    private void onTick(int currentTick){
        this.scheduler.onTick(currentTick);
    }

    @SneakyThrows
    public void shutdown() {
        this.shutdown = true;
        for (Map.Entry<UUID, ProxiedPlayer> player : this.playerManager.getPlayers().entrySet()) {
            this.logger.info("Disconnecting " + player.getValue().getName());
            player.getValue().disconnect("Proxy Shutdown", true);
        }
        Thread.sleep(500);

        this.console.getConsoleThread().interrupt();
        this.pluginManager.disableAllPlugins();
    }

    public String translate(TextContainer textContainer){
        return this.getLanguageConfig().translateContainer(textContainer);
    }

    public boolean handlePlayerCommand(ProxiedPlayer player, String message){
        if (!this.commandMap.handleMessage(player, message)){
            return false;
        }
        return this.dispatchCommand(player, message.substring(this.commandMap.getCommandPrefix().length()));
    }

    public boolean dispatchCommand(CommandSender sender, String message){
        DispatchCommandEvent event = new DispatchCommandEvent(sender, message);
        this.eventManager.callEvent(event);

        if (event.isCancelled()){
            return false;
        }
        String[] args = message.split(" ");
        return this.commandMap.handleCommand(sender, args[0], Arrays.copyOfRange(args, 1, args.length));
    }

    public boolean isRunning(){
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

    public LangConfig getLanguageConfig(){
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

    public boolean registerServerInfo(ServerInfo serverInfo) {
        if (serverInfo == null) return false;
        return this.serverInfoMap.putIfAbsent(serverInfo.getServerName().toLowerCase(), serverInfo) == null;
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
}
