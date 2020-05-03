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

package pe.waterdog.player;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.nukkitx.math.GenericMath;
import com.nukkitx.protocol.bedrock.BedrockClient;
import pe.waterdog.ProxyServer;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

public class PlayerManager {

    private final ProxyServer server;

    private final ConcurrentMap<UUID, ProxiedPlayer> players = new ConcurrentHashMap<>();

    private final ThreadPoolExecutor sessionTicker = new ThreadPoolExecutor(1, 1, 1, TimeUnit.MINUTES,
            new LinkedBlockingQueue<>(), new ThreadFactoryBuilder().setNameFormat("WaterdogPE - Session Ticker #%d").setDaemon(true).build());

    public PlayerManager(ProxyServer server){
        this.server = server;
    }

    public BedrockClient bindClient(){
        InetSocketAddress address = new InetSocketAddress("0.0.0.0", ThreadLocalRandom.current().nextInt(20000, 60000));
        BedrockClient client = new BedrockClient(address);

        client.bind().join();
        return client;
    }

    public boolean registerPlayer(ProxiedPlayer player){
        if (player == null) return false;

        ProxiedPlayer previousSession = this.players.put(player.getUniqueId(), player);
        boolean success = true;

        if (previousSession != null && !previousSession.getUpstream().isClosed()){
            success = false;

            previousSession.getUpstream().disconnect("disconnectionScreen.loggedinOtherLocation");
            player.getUpstream().disconnect("disconnectionScreen.loggedinOtherLocation");
        }

        this.adjustPoolSize();
        return success;
    }

    public void removePlayer(ProxiedPlayer player){
        if (player == null) return;

        this.players.remove(player.getUniqueId());
        this.adjustPoolSize();
    }

    public ProxiedPlayer getPlayer(UUID uuid){
        return uuid == null? null : this.players.get(uuid);
    }

    public ProxiedPlayer getPlayer(String playerName){
        if (playerName == null) return null;

        for (ProxiedPlayer player : this.players.values()){
            if (!player.getName().toLowerCase().startsWith(playerName)) continue;

            int strLen = player.getName().length() - playerName.length();
            if (strLen == 0) return player;
        }

        return null;
    }

    public Map<UUID, ProxiedPlayer> getPlayers() {
        return ImmutableMap.copyOf(this.players);
    }

    private void adjustPoolSize() {
        int threads = GenericMath.clamp(this.players.size() / 50, 1, Runtime.getRuntime().availableProcessors());
        if (sessionTicker.getMaximumPoolSize() != threads) {
            sessionTicker.setMaximumPoolSize(threads);
        }
    }
}
