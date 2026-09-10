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
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import org.cloudburstmc.protocol.bedrock.netty.BedrockBatchWrapper;

import java.util.List;

/**
 * Moves a compressed batch between the pipeline and whatever the transport puts on the wire.
 * <p>
 * Registered under the same name for every transport, which is what lets the handlers above it
 * stay transport agnostic.
 */
public abstract class TransportFrameCodec<T> extends MessageToMessageCodec<T, BedrockBatchWrapper> {
    public static final String NAME = "transport-frame-codec";

    @Override
    protected void encode(ChannelHandlerContext ctx, BedrockBatchWrapper msg, List<Object> out) {
        if (msg.getCompressed() == null) {
            throw new IllegalStateException("Bedrock batch was not compressed");
        }
        out.add(this.frame(ctx, msg.getCompressed().retainedSlice()));
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, T msg, List<Object> out) {
        ByteBuf body = this.unframe(ctx, msg);
        if (body == null || !body.isReadable()) {
            return;
        }
        out.add(BedrockBatchWrapper.newInstance(body.readRetainedSlice(body.readableBytes()), null));
    }

    /**
     * Wraps a compressed batch for the wire. Ownership of {@code batch} passes to the result.
     */
    protected abstract ByteBuf frame(ChannelHandlerContext ctx, ByteBuf batch);

    /**
     * Unwraps a transport message down to the batch, or returns null to drop it.
     */
    protected abstract ByteBuf unframe(ChannelHandlerContext ctx, T msg);
}
