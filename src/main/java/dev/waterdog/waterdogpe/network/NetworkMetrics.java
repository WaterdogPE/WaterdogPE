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
import org.cloudburstmc.protocol.bedrock.PacketDirection;

/**
 * This interface can be used to record and display WaterdogPE-Internal metrics.
 */
public interface NetworkMetrics {
    AttributeKey<NetworkMetrics> ATTRIBUTE = AttributeKey.newInstance("waterdog_metrics");

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
