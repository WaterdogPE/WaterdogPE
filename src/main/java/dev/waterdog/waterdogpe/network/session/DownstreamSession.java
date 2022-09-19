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
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import io.netty.buffer.ByteBuf;

import javax.crypto.SecretKey;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.function.Consumer;

public interface DownstreamSession {

    /**
     * Called once the connection was fully initialize and is ready to handle and send packets.
     * At this point downstream BatchBridge and packet handlers should be assigned.
     * @param player The instance of ProxiedPlayer who requested the connection
     * @param initial Whatever this is the first runtime server player has connected to
     */
    void onDownstreamInit(ProxiedPlayer player, boolean initial);

    /**
     * Called once the join phase of the first server is completed and rewrite maps can be used.
     * @param player The instance of ProxiedPlayer
     */
    void onInitialServerConnected(ProxiedPlayer player);

    /**
     * Called once the transfer phase has begun.
     * At this point all received packets should be queued (if using dimension change).
     * @param player The instance of ProxiedPlayer
     */
    void onServerConnected(ProxiedPlayer player);

    /**
     * Called once the transfer phase is completed and player is fully initialized on new downstream server.
     * @param player The instance of ProxiedPlayer
     * @param completedCallback Callback instance which MUST be called at the END of the implementation of thsi method.
     */
    void onTransferCompleted(ProxiedPlayer player, Runnable completedCallback);

    /**
     * Adds disconnect handler to the current session.
     * @param handler Consumer accepting reason which is called after session disconnects
     */
    void addDisconnectHandler(Consumer<Object> handler);

    /**
     * Sends packet to the downstream
     * @param packet Packet to be send
     */
    void sendPacket(BedrockPacket packet);

    /**
     * Sends packet immediately to the downstream
     * @param packet Packet to be send
     */
    void sendPacketImmediately(BedrockPacket packet);

    /**
     * Sends the given packet batch to the downstream server.
     * @param packets Collection of packets
     * @param encrypt Whether the batch should have encryption enabled or not
     */
    void sendWrapped(Collection<BedrockPacket> packets, boolean encrypt);

    /**
     * Sends network compressed batch to the downstream.
     * @param compressed Compressed buffer
     * @param encrypt Whatever encrypt the buffer
     */
    void sendWrapped(ByteBuf compressed, boolean encrypt);

    /**
     * Configures and enables the encryption after ServerToCLient handshake
     * @param secretKey Encryption key
     */
    default void enableEncryption(SecretKey secretKey) {
        throw new UnsupportedOperationException("Encryption is not implemented!");
    }

    default boolean isEncrypted() {
        return false;
    }

    /**
     * Gets the instance of the Shield Blocking ID which can be found during deserialization of the StartGamePacket.
     * This ID is required for packets that contain ItemStacks.
     * @return Item ID of the shield
     */
    int getHardcodedBlockingId();

    /**
     * Gets the remote address of teh connection.
     * @return Remote address
     */
    InetSocketAddress getAddress();

    /**
     * Gets the connection latency.
     * @return Latency in MS
     */
    long getLatency();

    /**
     * Gets the compression algorithm used by this session
     * @return Compression algorithm
     */
    CompressionAlgorithm getCompression();

    /**
     * Sets the compression algorIthm
     * @param compression compression to be used
     */
    void setCompression(CompressionAlgorithm compression);

    /**
     * Checks if the session is ready for communication.
     * @return Whatever session was not closed
     */
    boolean isClosed();

    /**
     * Disconnects from the remote server. Should send DisconnectNotification.
     */
    void disconnect();

}
