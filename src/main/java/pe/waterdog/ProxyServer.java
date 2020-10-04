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
import org.slf4j.Logger;
import pe.waterdog.command.CommandReader;
import pe.waterdog.network.ProxyListener;
import pe.waterdog.network.ServerInfo;
import pe.waterdog.player.PlayerManager;
import pe.waterdog.player.ProxiedPlayer;
import pe.waterdog.utils.ConfigurationManager;
import pe.waterdog.utils.ProxyConfig;

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
    private PlayerManager playerManager;

    private boolean shutdown = false;

    private Map<String, ServerInfo> serverInfoMap;

    public ProxyServer(Logger logger, String filePath, String pluginPath) {
        instance = this;
        this.logger = logger;
        this.dataPath = Paths.get(filePath);

       /* if (!new File(pluginPath).exists()) {
            new File(pluginPath).mkdirs();
        }

        this.pluginPath = new File(pluginPath).getAbsolutePath() + "/";*/

       /*this.console = new CommandReader();
       this.console.start();*/

        this.configurationManager = new ConfigurationManager(this);
        configurationManager.loadProxyConfig();

        this.serverInfoMap = configurationManager.getProxyConfig().buildServerMap();

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
    }

    private void tickProcessor() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
        while (!this.shutdown) {
            try {
                synchronized (this) {
                    this.wait();
                }
            } catch (InterruptedException e) {
                //ignore
            }
        }


    }

    @SneakyThrows
    public void shutdown() {
        for (Map.Entry<UUID, ProxiedPlayer> player : getPlayerManager().getPlayers().entrySet()) {
            System.out.println("Disconnecting " + player.getValue().getName());
            player.getValue().disconnect("Proxy Shutdown", true);
        }
        Thread.sleep(500);
        this.shutdown = true;
    }

    public Logger getLogger() {
        return logger;
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

    public PlayerManager getPlayerManager() {
        return playerManager;
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
}
