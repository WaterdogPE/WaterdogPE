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

package dev.waterdog.waterdogpe.network.serverinfo;

import dev.waterdog.waterdogpe.network.connection.client.ClientConnection;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import io.netty.util.concurrent.Future;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSets;
import lombok.ToString;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Set;

/**
 * Base informative class for servers.
 * Every server registered to the Proxy has one instance of this class, holding its name, and it's address (ip&port)
 * Also holds a list of all ProxiedPlayers connected.
 */
@ToString(exclude = {"players"})
public abstract class ServerInfo {

    private final String serverName;
    private final InetSocketAddress address;
    private final InetSocketAddress publicAddress;

    private final Set<ClientConnection> connections = ObjectSets.synchronize(new ObjectOpenHashSet<>());
    private final Set<ProxiedPlayer> players = ObjectSets.synchronize(new ObjectOpenHashSet<>());

    public ServerInfo(String serverName, InetSocketAddress address, InetSocketAddress publicAddress) {
        this.serverName = serverName;
        this.address = address;
        if (publicAddress == null) {
            publicAddress = address;
        }
        this.publicAddress = publicAddress;
    }

    public abstract ServerInfoType getServerType();
    public abstract Future<ClientConnection> createConnection(ProxiedPlayer player);

    public boolean matchAddress(String address, int port) {
        InetAddress inetAddress = this.publicAddress.getAddress();
        boolean addressMatch;
        if (inetAddress == null) {
            addressMatch = (this.publicAddress.getHostString().equals(address) || this.publicAddress.getHostName().equals(address));
        } else {
            addressMatch = (inetAddress.getHostAddress().equals(address) || inetAddress.getHostName().equals(address));
        }
        return addressMatch && this.publicAddress.getPort() == port;
    }

    public void addConnection(ClientConnection connection) {
        if (connection != null && connection.isConnected()) {
            this.players.add(connection.getPlayer());
            this.connections.add(connection);
        }
    }

    public void removeConnection(ClientConnection connection) {
        if (connection != null) {
            this.connections.remove(connection);
            this.players.remove(connection.getPlayer());
        }
    }

    public Set<ProxiedPlayer> getPlayers() {
        return Collections.unmodifiableSet(this.players);
    }

    public Set<ClientConnection> getConnections() {
        return Collections.unmodifiableSet(this.connections);
    }

    public String getServerName() {
        return this.serverName;
    }

    public InetSocketAddress getAddress() {
        return this.address;
    }

    public InetSocketAddress getPublicAddress() {
        return this.publicAddress;
    }
}
