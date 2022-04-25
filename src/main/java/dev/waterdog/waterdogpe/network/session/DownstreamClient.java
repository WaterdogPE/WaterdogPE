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

package dev.waterdog.waterdogpe.network.session;

import com.nukkitx.protocol.bedrock.BedrockPacket;
import dev.waterdog.waterdogpe.network.bridge.UpstreamBridge;
import dev.waterdog.waterdogpe.network.protocol.ProtocolVersion;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Interface which is used to create custom downstream connection.
 * This allows custom packet serialization and opens much more options to the future.
 */
public interface DownstreamClient {

    /**
     * This method should be used to setup client and make everything ready for new connection.
     * @param protocol version of protocol which will be used
     * @return future instance if 'this' which is completed once everything is configured
     */
    CompletableFuture<DownstreamClient> bindDownstream(ProtocolVersion protocol);

    default CompletableFuture<DownstreamSession> connect(InetSocketAddress address) {
        return this.connect(address, 15, TimeUnit.SECONDS);
    }

    /**
     * After successful binding this method can be called to fully establish the connection and open new session.
     * @param address downstream server address
     * @param timeout value of time in 'unit' when the connection times-out
     * @param unit TimeUnit representation of timeout
     * @return future instance of newly created DownstreamSession
     */
    CompletableFuture<DownstreamSession> connect(InetSocketAddress address, long timeout, TimeUnit unit);

    /**
     * Sends packet to the DownstreamSession
     * @param packet The packet to be sent
     */
    default void sendPacket(BedrockPacket packet) {
        this.getSession().sendPacket(packet);
    }

    /**
     * Sends packet immediately to the DownstreamSession
     * @param packet The packet to be sent
     */
    default void sendPacketImmediately(BedrockPacket packet) {
        this.getSession().sendPacketImmediately(packet);
    }

    /**
     * This method allows creating own customized implementation of the UpstreamBridge
     * @param player the instance if ProxiedPlayer passed to created UpstreamBridge
     * @return new instance of UpstreamBridge
     */
    default UpstreamBridge newUpstreamBridge(ProxiedPlayer player) {
        return new UpstreamBridge(player, this.getSession());
    }

    /**
     * Returns address which is this client bind to.
     * @return local address
     */
    InetSocketAddress getBindAddress();

    default void close() {
        this.close(false);
    }

    /**
     * Close the session and the whole connection.
     * @param force whatever block current thread untill everything is closed
     */
    void close(boolean force);

    /**
     * Checks if the current client was initialized and connected.
     * @return Returns true if DownstreamSession was initialized and connection is opened.
     */
    boolean isConnected();

    /**
     * Gets the instance of ServerInfo which created this client.
     * @return ServerInfo instance
     */
    ServerInfo getServerInfo();

    /**
     * Gets downstream session
     * @return instance of DownstreamSession which was created during connection
     */
    DownstreamSession getSession();
}
