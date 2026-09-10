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
import org.cloudburstmc.netty.channel.raknet.RakReliability;
import org.cloudburstmc.netty.channel.raknet.packet.RakMessage;

/**
 * RakNet carries the batch behind a single frame id byte.
 */
@ChannelHandler.Sharable
public class RakNetFrameCodec extends TransportFrameCodec<RakMessage> {
    public static final int FRAME_ID = 0xfe;

    @Override
    protected ByteBuf frame(ChannelHandlerContext ctx, ByteBuf batch) {
        CompositeByteBuf buf = ctx.alloc().compositeDirectBuffer(2);
        try {
            buf.addComponent(true, ctx.alloc().ioBuffer(1).writeByte(FRAME_ID));
            buf.addComponent(true, batch);
            return buf.retain();
        } finally {
            buf.release();
        }
    }

    @Override
    protected ByteBuf unframe(ChannelHandlerContext ctx, RakMessage msg) {
        if (msg.channel() != 0 && msg.reliability() != RakReliability.RELIABLE_ORDERED) {
            return null;
        }
        ByteBuf content = msg.content();
        if (!content.isReadable()) {
            return null;
        }
        int id = content.readUnsignedByte();
        if (id != FRAME_ID) {
            throw new IllegalStateException("Invalid frame ID: " + id);
        }
        return content;
    }
}
