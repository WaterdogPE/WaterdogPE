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

import com.nukkitx.protocol.bedrock.BedrockServer;
import lombok.SneakyThrows;
import pe.waterdog.command.CommandReader;
import pe.waterdog.logger.Logger;
import pe.waterdog.network.ProxyListener;
import pe.waterdog.network.ServerInfo;
import pe.waterdog.player.PlayerManager;
import pe.waterdog.player.ProxiedPlayer;
import pe.waterdog.plugin.Plugin;
import pe.waterdog.plugin.PluginManager;
import pe.waterdog.scheduler.WaterdogScheduler;
import pe.waterdog.utils.ConfigurationManager;
import pe.waterdog.utils.ProxyConfig;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

public class ProxyServer {

    private static ProxyServer instance;

    private Path dataPath;
    private Path pluginPath;

    private Logger logger;
    private CommandReader console;

    private BedrockServer bedrockServer;

    private ConfigurationManager configurationManager;
    private WaterdogScheduler scheduler;
    private PlayerManager playerManager;
    private PluginManager pluginManager;
    private boolean shutdown = false;

    private Map<String, ServerInfo> serverInfoMap;

    private int currentTick = 0;
    private long nextTick;

    public ProxyServer(Logger logger, String filePath, String pluginPath) {
        instance = this;
        this.logger = logger;
        this.dataPath = Paths.get(filePath);
        this.pluginPath = Paths.get(pluginPath);

        if (!new File(pluginPath).exists()) {
            this.logger.info("Created Plugin Folder at " + this.pluginPath.toString());
            new File(pluginPath).mkdirs();
        }

        this.pluginManager = new PluginManager(this);


       /*this.console = new CommandReader();
       this.console.start();*/

        this.configurationManager = new ConfigurationManager(this);
        configurationManager.loadProxyConfig();

        this.serverInfoMap = configurationManager.getProxyConfig().buildServerMap();

        this.scheduler = new WaterdogScheduler(this);
        this.playerManager = new PlayerManager(this);

        this.boot();
        this.tickProcessor();
    }

    public static ProxyServer getInstance() {
        return instance;
    }

    private void boot() {
        InetSocketAddress bindAddress = this.getConfiguration().getBindAddress();
        this.logger.info("Binding to " + bindAddress);

        this.bedrockServer = new BedrockServer(bindAddress, Runtime.getRuntime().availableProcessors());
        bedrockServer.setHandler(new ProxyListener(this));
        bedrockServer.bind().join();

        for (Plugin plugin : pluginManager.getPlugins()) {
            plugin.onEnable();
        }
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
    }

    private void onTick(int currentTick){
        this.scheduler.onTick(currentTick);
    }

    @SneakyThrows
    public void shutdown() {
        for (Map.Entry<UUID, ProxiedPlayer> player : this.playerManager.getPlayers().entrySet()) {
            this.logger.info("Disconnecting " + player.getValue().getName());
            player.getValue().disconnect("Proxy Shutdown", true);
        }
        Thread.sleep(500);

        for (Plugin plugin : pluginManager.getPlugins()) {
            plugin.onShutdown();
        }
        this.shutdown = true;
    }

    public Logger getLogger() {
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

    public WaterdogScheduler getScheduler() {
        return this.scheduler;
    }

    public PlayerManager getPlayerManager() {
        return this.playerManager;
    }

    public ProxiedPlayer getPlayer(UUID uuid) {
        return this.playerManager.getPlayer(uuid);
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
        return pluginPath;
    }

    public PluginManager getPluginManager() {
        return pluginManager;
    }
  
    public int getCurrentTick() {
        return this.currentTick;
    }
}
