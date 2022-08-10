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

package dev.waterdog.waterdogpe.network.serverinfo;

import com.nukkitx.network.raknet.RakNetPong;
import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.network.protocol.ProtocolConstants;
import dev.waterdog.waterdogpe.network.protocol.ProtocolVersion;
import dev.waterdog.waterdogpe.network.session.DownstreamClient;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSets;
import lombok.ToString;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Base informative class for servers.
 * Every server registered to the Proxy has one instance of this class, holding its name aswell as its address(ip&port)
 * Also holds a list of all ProxiedPlayers connected.
 */
@ToString(exclude = {"players"})
public abstract class ServerInfo {

    private final String serverName;
    private final InetSocketAddress address;
    private final InetSocketAddress publicAddress;

    private final Set<ProxiedPlayer> players = ObjectSets.synchronize(new ObjectOpenHashSet<>());

    public ServerInfo(String serverName, InetSocketAddress address, InetSocketAddress publicAddress) {
        this.serverName = serverName;
        this.address = address;
        if (publicAddress == null) {
            publicAddress = address;
        }
        this.publicAddress = publicAddress;
    }

    /**
     * CompletableFuture may throw exception if ping fails. Therefore it is recommended to handle using whenComplete().
     * @return CompletableFuture with RakNetPong.
     */
    public CompletableFuture<RakNetPong> ping(long timeout, TimeUnit unit) {
        return ProxyServer.getInstance().bindClient(ProtocolConstants.getLatestProtocol()).thenCompose(client ->
                client.getRakNet().ping(this.address, timeout, unit).whenComplete((pong, error) -> client.close()));
    }

    public abstract DownstreamClient createNewConnection(ProtocolVersion protocol);

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

    public void addPlayer(ProxiedPlayer player) {
        if (player != null && player.isConnected()) {
            this.players.add(player);
        }
    }

    public void removePlayer(ProxiedPlayer player) {
        this.players.remove(player);
    }

    public Set<ProxiedPlayer> getPlayers() {
        return Collections.unmodifiableSet(this.players);
    }

    public abstract ServerInfoType getServerType();

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
