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

package dev.waterdog.waterdogpe.network.connection.codec.packet;

import dev.waterdog.waterdogpe.network.NetworkMetrics;
import dev.waterdog.waterdogpe.network.protocol.ProtocolVersion;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.cloudburstmc.protocol.bedrock.PacketDirection;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.netty.BedrockBatchWrapper;
import org.cloudburstmc.protocol.bedrock.netty.BedrockPacketWrapper;
import org.cloudburstmc.protocol.bedrock.packet.SetTimePacket;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BedrockPacketCodecMetricsTest {

    private static final BedrockCodec CODEC = ProtocolVersion.latest().getDefaultCodec();

    @Test
    public void countsEncodedAndPassedThroughPacketsApart() {
        AtomicInteger passedThrough = new AtomicInteger();
        AtomicInteger encoded = new AtomicInteger();
        NetworkMetrics metrics = new NetworkMetrics() {
            @Override
            public void passedThroughPackets(int count, PacketDirection direction) {
                passedThrough.addAndGet(count);
            }

            @Override
            public void encodedPackets(int count, PacketDirection direction) {
                encoded.addAndGet(count);
            }
        };

        EmbeddedChannel channel = new EmbeddedChannel();
        channel.attr(NetworkMetrics.ATTRIBUTE).set(metrics);
        channel.attr(PacketDirection.ATTRIBUTE).set(PacketDirection.SERVER_BOUND);
        BedrockPacketCodec codec = new BedrockPacketCodec_v3();
        channel.pipeline().addLast(codec);
        codec.setCodecHelper(CODEC, CODEC.createHelper());

        // Two already carry a buffer and pass through, one has to be encoded
        BedrockBatchWrapper batch = BedrockBatchWrapper.newInstance();
        batch.addPacket(BedrockPacketWrapper.create(0, 0, 0, null, Unpooled.buffer().writeByte(1)));
        batch.addPacket(BedrockPacketWrapper.create(0, 0, 0, null, Unpooled.buffer().writeByte(1)));
        SetTimePacket time = new SetTimePacket();
        time.setTime(1000);
        batch.addPacket(BedrockPacketWrapper.create(0, 0, 0, time, null));
        channel.writeOutbound(batch);

        assertEquals(2, passedThrough.get());
        assertEquals(1, encoded.get());
        channel.finishAndReleaseAll();
    }
}
