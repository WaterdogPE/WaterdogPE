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
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import org.cloudburstmc.protocol.bedrock.netty.BedrockBatchWrapper;
import org.cloudburstmc.protocol.bedrock.netty.BedrockPacketWrapper;
import org.cloudburstmc.protocol.common.util.VarInts;

import java.util.List;

public class BedrockBatchEncoder extends MessageToMessageEncoder<BedrockBatchWrapper> {
    public static final String NAME = "bedrock-batch-encoder";

    @Override
    protected void encode(ChannelHandlerContext ctx, BedrockBatchWrapper msg, List<Object> out) {
        if (!msg.isModified() && (msg.getCompressed() != null || msg.getUncompressed() != null)) {
            out.add(msg.retain());
            return;
        }

        CompositeByteBuf buf = ctx.alloc().compositeDirectBuffer(msg.getPackets().size() * 2);
        try {
            for (BedrockPacketWrapper packet : msg.getPackets()) {
                ByteBuf message = packet.getPacketBuffer();
                if (message == null) {
                    throw new IllegalArgumentException("BedrockPacket is not encoded");
                }

                ByteBuf header = ctx.alloc().ioBuffer(5);
                VarInts.writeUnsignedInt(header, message.readableBytes());
                buf.addComponent(true, header);
                buf.addComponent(true, message.retainedSlice());
            }
            msg.setUncompressed(buf.retain());
        } finally {
            buf.release();
        }
        out.add(msg.retain());
    }
}
