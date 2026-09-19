/*
 * Copyright 2023 WaterdogTEAM
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

package dev.waterdog.waterdogpe.network;

import io.netty.util.AttributeKey;
import org.cloudburstmc.netty.channel.nethernet.config.NetherChannelMetrics;
import org.cloudburstmc.netty.channel.nethernet.config.NetherServerMetrics;
import org.cloudburstmc.netty.channel.raknet.config.RakChannelMetrics;
import org.cloudburstmc.netty.channel.raknet.config.RakServerMetrics;
import org.cloudburstmc.protocol.bedrock.PacketDirection;

/**
 * This interface can be used to record and display WaterdogPE-Internal metrics.
 */
public interface NetworkMetrics {
    AttributeKey<NetworkMetrics> ATTRIBUTE = AttributeKey.newInstance("waterdog_metrics");

    /**
     * Which side of the proxy a connection sits on. The same metrics object serves both, so
     * anything that wants to tell them apart has to be handed the leg.
     */
    enum Leg {
        /** Player to proxy. */
        UPSTREAM,
        /** Proxy to downstream server. */
        DOWNSTREAM
    }

    /**
     * The transport metrics to install on a RakNet connection, or null to install none.
     * <p>
     * Returning a different object per leg is the only way to keep player traffic apart from
     * downstream traffic, since both legs share this one metrics instance.
     *
     * @param leg which side the connection is on
     */
    default RakChannelMetrics rakMetrics(Leg leg) {
        // Kept for implementations written before the leg was passed.
        return this instanceof RakChannelMetrics metrics ? metrics : null;
    }

    /**
     * The transport metrics to install on a NetherNet connection, or null to install none.
     *
     * @param leg which side the connection is on
     */
    default NetherChannelMetrics netherMetrics(Leg leg) {
        return this instanceof NetherChannelMetrics metrics ? metrics : null;
    }

    /**
     * The listener wide RakNet metrics, or null for none. Reports what happens before a connection
     * exists, such as a blocked address or a bad cookie.
     */
    default RakServerMetrics rakServerMetrics() {
        return this instanceof RakServerMetrics metrics ? metrics : null;
    }

    /**
     * The listener wide NetherNet metrics, or null for none. Reports joins refused at signaling and
     * connections that fail before they carry traffic.
     */
    default NetherServerMetrics netherServerMetrics() {
        return this instanceof NetherServerMetrics metrics ? metrics : null;
    }

    /**
     * Triggered when the packet queue of the TransferBatchBridge becomes too large and the player gets disconnected
     */
    default void packetQueueTooLarge() {
    }

    /**
     * Called once a BedrockBatchWrapper is compressed.
     * @param count the amount of bytes being compressed
     * @param direction the packet direction
     */
    default void compressedBytes(int count, PacketDirection direction) {
    }

    /**
     * Called once a BedrockBatchWrapper is decompressed.
     * @param count the amount of bytes being decompressed
     * @param direction the packet direction
     */
    default void decompressedBytes(int count, PacketDirection direction) {

    }

    /**
     * Called once a BedrockBatchWrapper doesn't need to be compressed again.
     * @param count the amount of compressed bytes being passed through
     * @param direction the packet direction
     */
    default void passedThroughBytes(int count, PacketDirection direction) {
    }

    /**
     * Called when a packet modified and is encoded.
     * @param count the amount of encoded packets
     * @param direction the packet direction
     */
    default void encodedPackets(int count, PacketDirection direction) {
    }

    /**
     * Called when a packet was not modified and is passed thought.
     * @param count the amount of passed through packets
     * @param direction the packet direction
     */
    default void passedThroughPackets(int count, PacketDirection direction) {
    }

    /**
     * Called when a datagram packet is dropped because it was blocked
     * @param count the amount of bytes within dropped datagram packet
     */
    default void droppedBytes(int count) {
    }
}
