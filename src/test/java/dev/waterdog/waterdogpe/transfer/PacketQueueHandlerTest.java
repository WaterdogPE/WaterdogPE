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

package dev.waterdog.waterdogpe.transfer;

import dev.waterdog.waterdogpe.network.connection.codec.batch.BatchFlags;
import dev.waterdog.waterdogpe.network.connection.codec.server.PacketQueueHandler;
import dev.waterdog.waterdogpe.network.connection.peer.BedrockServerSession;
import io.netty.channel.embedded.EmbeddedChannel;
import org.cloudburstmc.protocol.bedrock.netty.BedrockBatchWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class PacketQueueHandlerTest {

    private BedrockServerSession session;
    private PacketQueueHandler handler;
    private EmbeddedChannel channel;

    @BeforeEach
    void setUp() {
        this.session = mock(BedrockServerSession.class);
        this.handler = new PacketQueueHandler(this.session);
        this.channel = new EmbeddedChannel();
        this.channel.pipeline().addLast(PacketQueueHandler.NAME, this.handler);
    }

    @Test
    void queuesBatchesInsteadOfWritingThem() {
        BedrockBatchWrapper batch = BedrockBatchWrapper.newInstance();
        this.channel.writeOutbound(batch);

        assertNull(this.channel.readOutbound(), "batch should be held in the queue");
        assertEquals(1, batch.refCnt());
    }

    @Test
    void flushesQueueOnRemovalWhileActive() {
        BedrockBatchWrapper batch = BedrockBatchWrapper.newInstance();
        this.channel.writeOutbound(batch);

        this.channel.pipeline().remove(PacketQueueHandler.NAME);

        assertSame(batch, this.channel.readOutbound(), "queued batch should be flushed to the client");
    }

    @Test
    void dropsQueueOnRemovalWhenDiscarded() {
        BedrockBatchWrapper batch = BedrockBatchWrapper.newInstance();
        this.channel.writeOutbound(batch);

        this.handler.dropQueued();
        this.channel.pipeline().remove(PacketQueueHandler.NAME);

        assertNull(this.channel.readOutbound(), "discarded batch must never reach the client");
        assertEquals(0, batch.refCnt(), "discarded batch should be released");
    }

    @Test
    void skipQueueFlagBypassesQueue() {
        BedrockBatchWrapper batch = BedrockBatchWrapper.newInstance();
        batch.setFlag(BatchFlags.SKIP_QUEUE);
        this.channel.writeOutbound(batch);

        assertSame(batch, this.channel.readOutbound(), "flagged batch should pass through immediately");
    }

    @Test
    void dropsQueueWhenChannelGoesInactive() {
        BedrockBatchWrapper batch = BedrockBatchWrapper.newInstance();
        this.channel.writeOutbound(batch);

        this.channel.close();

        assertNull(this.channel.readOutbound());
        assertEquals(0, batch.refCnt());
    }

    @Test
    void overflowDisconnectsSession() {
        // MAX_BATCHES is 256, but netty clamps MPSC queues to a minimum capacity of 2048.
        for (int i = 0; i < 3000 && this.channel.pipeline().get(PacketQueueHandler.NAME) != null; i++) {
            this.channel.writeOutbound(BedrockBatchWrapper.newInstance());
        }
        verify(this.session, atLeastOnce()).disconnect(any(CharSequence.class));
    }
}