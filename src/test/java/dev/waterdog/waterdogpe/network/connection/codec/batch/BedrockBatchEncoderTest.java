/*
 * Copyright 2026 WaterdogTEAM
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

package dev.waterdog.waterdogpe.network.connection.codec.batch;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import org.cloudburstmc.netty.channel.TransportChannel;
import org.cloudburstmc.protocol.bedrock.netty.BedrockBatchWrapper;
import org.cloudburstmc.protocol.bedrock.netty.BedrockPacketWrapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BedrockBatchEncoderTest {

    private static BedrockBatchWrapper batch(int... lengths) {
        BedrockBatchWrapper batch = BedrockBatchWrapper.newInstance();
        for (int length : lengths) {
            BedrockPacketWrapper packet = BedrockPacketWrapper.create();
            packet.setPacketBuffer(Unpooled.buffer(length).writeZero(length));
            batch.addPacket(packet);
        }
        return batch;
    }

    // A transport taking messages of up to 1,024,000 bytes, of which batches leave a 64th for compression
    private static final int MAX_MESSAGE_SIZE = 1_024_000;
    private static final int MAX_BATCH_SIZE = 1_008_000;

    private static List<BedrockBatchWrapper> encode(BedrockBatchWrapper batch) {
        return encode(new TransportEmbeddedChannel(new BedrockBatchEncoder()), batch);
    }

    private static List<BedrockBatchWrapper> encode(EmbeddedChannel channel, BedrockBatchWrapper batch) {
        channel.writeOutbound(batch);
        List<BedrockBatchWrapper> encoded = new ArrayList<>();
        BedrockBatchWrapper next;
        while ((next = channel.readOutbound()) != null) {
            encoded.add(next);
        }
        assertFalse(channel.finish());
        return encoded;
    }

    private static void assertPacketCounts(List<BedrockBatchWrapper> encoded, int maxBatchSize, int... packetCounts) {
        try {
            assertEquals(packetCounts.length, encoded.size());
            for (int i = 0; i < packetCounts.length; i++) {
                BedrockBatchWrapper batch = encoded.get(i);
                assertEquals(packetCounts[i], batch.getPackets().size());
                if (packetCounts[i] > 1) {
                    assertTrue(batch.getUncompressed().readableBytes() <= maxBatchSize);
                }
            }
        } finally {
            encoded.forEach(BedrockBatchWrapper::release);
        }
    }

    @Test
    void keepsABatchWithinTheLimitWhole() {
        assertPacketCounts(encode(batch(1, 2, 3)), MAX_BATCH_SIZE, 3);
    }

    @Test
    void splitsABatchOverTheLimit() {
        int[] lengths = new int[10];
        Arrays.fill(lengths, 300_000);
        assertPacketCounts(encode(batch(lengths)), MAX_BATCH_SIZE, 3, 3, 3, 1);
    }

    @Test
    void sendsAnOversizedPacketAlone() {
        assertPacketCounts(encode(batch(16, MAX_BATCH_SIZE + 1, 16)), MAX_BATCH_SIZE, 1, 1, 1);
    }

    @Test
    void leavesBatchesWholeWithoutATransportLimit() {
        int[] lengths = new int[10];
        Arrays.fill(lengths, 300_000);
        assertPacketCounts(encode(new EmbeddedChannel(new BedrockBatchEncoder()), batch(lengths)), Integer.MAX_VALUE, 10);
    }

    @Test
    void keepsFlagsOnSplitBatches() {
        BedrockBatchWrapper batch = batch(MAX_BATCH_SIZE, 16);
        batch.setFlag(BatchFlags.SKIP_QUEUE);
        List<BedrockBatchWrapper> encoded = encode(batch);
        try {
            assertEquals(2, encoded.size());
            encoded.forEach(split -> assertTrue(split.hasFlag(BatchFlags.SKIP_QUEUE)));
        } finally {
            encoded.forEach(BedrockBatchWrapper::release);
        }
    }

    private static final class TransportEmbeddedChannel extends EmbeddedChannel implements TransportChannel {
        TransportEmbeddedChannel(ChannelHandler... handlers) {
            super(handlers);
        }

        @Override
        public int maxMessageSize() {
            return MAX_MESSAGE_SIZE;
        }

        @Override
        public long getPing() {
            return 0;
        }
    }
}
