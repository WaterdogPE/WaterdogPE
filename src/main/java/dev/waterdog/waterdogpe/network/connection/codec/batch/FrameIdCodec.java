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

package dev.waterdog.waterdogpe.network.connection.codec.batch;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import org.cloudburstmc.netty.channel.raknet.RakReliability;
import org.cloudburstmc.netty.channel.raknet.packet.RakMessage;
import org.cloudburstmc.protocol.bedrock.netty.BedrockBatchWrapper;

import java.util.List;
import java.util.function.Function;

@ChannelHandler.Sharable
public abstract class FrameIdCodec<T> extends MessageToMessageCodec<T, BedrockBatchWrapper> {
    public static final String NAME = "frame-id-codec";

    private final int frameId;

    public FrameIdCodec(int frameId) {
        this.frameId = frameId;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, BedrockBatchWrapper msg, List<Object> out) throws Exception {
        if (msg.getCompressed() == null) {
            throw new IllegalStateException("Bedrock batch was not compressed");
        }

        CompositeByteBuf buf = ctx.alloc().compositeDirectBuffer(2);
        try {
            buf.addComponent(true, ctx.alloc().ioBuffer(1).writeByte(frameId));
            buf.addComponent(true, msg.getCompressed().retainedSlice());
            out.add(buf.retain());
        } finally {
            buf.release();
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, T msg, List<Object> out) throws Exception {
        ByteBuf byteBuf = this.decode0(ctx, msg);
        if (byteBuf == null || !byteBuf.isReadable()) {
            return;
        }

        int id = byteBuf.readUnsignedByte();
        if (id != frameId) {
            throw new IllegalStateException("Invalid frame ID: " + id);
        }
        out.add(BedrockBatchWrapper.newInstance(byteBuf.readRetainedSlice(byteBuf.readableBytes()), null));
    }

    protected abstract ByteBuf decode0(ChannelHandlerContext ctx, T msg);

    public static final Function<Integer, FrameIdCodec<ByteBuf>> BUFFER_CODEC = frameId -> new FrameIdCodec<>(frameId) {
        @Override
        protected ByteBuf decode0(ChannelHandlerContext ctx, ByteBuf msg) {
            return msg;
        }
    };

    public static final Function<Integer, FrameIdCodec<RakMessage>> RAK_CODEC = frameId -> new FrameIdCodec<>(frameId) {
        @Override
        protected ByteBuf decode0(ChannelHandlerContext ctx, RakMessage msg) {
            if (msg.channel() != 0 && msg.reliability() != RakReliability.RELIABLE_ORDERED) {
                return null;
            }
            return msg.content();
        }
    };
}
