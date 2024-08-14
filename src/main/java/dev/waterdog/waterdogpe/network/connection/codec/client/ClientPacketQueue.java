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

package dev.waterdog.waterdogpe.network.connection.codec.client;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.ScheduledFuture;
import io.netty.util.internal.PlatformDependent;
import org.cloudburstmc.protocol.bedrock.netty.BedrockBatchWrapper;
import org.cloudburstmc.protocol.bedrock.netty.BedrockPacketWrapper;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;

import java.util.Queue;
import java.util.concurrent.TimeUnit;

public class ClientPacketQueue extends ChannelDuplexHandler {
    public static final String NAME = "client-packet-queue";

    private final Queue<BedrockPacketWrapper> packetQueue = PlatformDependent.newMpscQueue();
    private ScheduledFuture<?> tickFuture;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.tickFuture = ctx.channel().eventLoop().scheduleAtFixedRate(() -> this.onTick(ctx), 50, 50, TimeUnit.MILLISECONDS);
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        this.tickFuture.cancel(false);
        this.tickFuture = null;
        super.channelInactive(ctx);
    }

    private void onTick(ChannelHandlerContext ctx) {
        if (!this.packetQueue.isEmpty()) {
            BedrockBatchWrapper batch = BedrockBatchWrapper.newInstance();

            BedrockPacketWrapper packet;
            while ((packet = this.packetQueue.poll()) != null) {
                batch.getPackets().add(packet);
            }
            ctx.writeAndFlush(batch);
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof BedrockPacket packet) {
            this.packetQueue.add(BedrockPacketWrapper.create(0, 0, 0, ReferenceCountUtil.retain(packet), null));
        } else if (msg instanceof BedrockPacketWrapper packet) {
            this.packetQueue.add(ReferenceCountUtil.retain(packet));
        } else if (msg instanceof BedrockBatchWrapper) {
            this.onTick(ctx);
            ctx.write(msg, promise);
        } else {
            ctx.write(msg, promise);
        }
    }
}
