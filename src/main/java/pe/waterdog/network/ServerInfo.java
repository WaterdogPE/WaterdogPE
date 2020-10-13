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

package pe.waterdog.network;

import pe.waterdog.player.ProxiedPlayer;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Base informative class for servers.
 * Every server registered to the Proxy has one instance of this class, holding its name aswell as its address(ip&port)
 * Also holds a list of all ProxiedPlayers connected.
 */
public class ServerInfo {

    private final String serverName;
    private final InetSocketAddress address;

    private List<ProxiedPlayer> players = new ArrayList<>();

    public ServerInfo(String serverName, InetSocketAddress address) {
        this.serverName = serverName;
        this.address = address;
    }

    public void addPlayer(ProxiedPlayer player) {
        if (player == null) return;
        this.players.add(player);
    }

    public void removePlayer(ProxiedPlayer player) {
        this.players.remove(player);
    }

    public List<ProxiedPlayer> getPlayers() {
        return this.players;
    }

    public String getServerName() {
        return this.serverName;
    }

    public InetSocketAddress getAddress() {
        return this.address;
    }
}
