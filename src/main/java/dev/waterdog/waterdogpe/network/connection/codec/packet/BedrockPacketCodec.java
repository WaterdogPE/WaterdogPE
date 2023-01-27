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

package dev.waterdog.waterdogpe.network.connection.codec.packet;

import dev.waterdog.waterdogpe.network.NetworkMetrics;
import dev.waterdog.waterdogpe.network.PacketDirection;
import dev.waterdog.waterdogpe.network.connection.codec.BedrockBatchWrapper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodecHelper;
import org.cloudburstmc.protocol.bedrock.codec.compat.BedrockCompat;
import org.cloudburstmc.protocol.bedrock.netty.BedrockPacketWrapper;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.UnknownPacket;

import java.util.List;

@Log4j2
public abstract class BedrockPacketCodec extends MessageToMessageCodec<BedrockBatchWrapper, BedrockBatchWrapper> {
    public static final String NAME = "bedrock-packet-codec";

    private BedrockCodec codec = BedrockCompat.CODEC;
    private BedrockCodecHelper helper = codec.createHelper();

    @Override
    protected void encode(ChannelHandlerContext ctx, BedrockBatchWrapper msg, List<Object> out) throws Exception {
        if (msg.isModified() || msg.getUncompressed() == null) {
            int passedThought = 0;
            int encodedPackets = 0;
            for (BedrockPacketWrapper packet : msg.getPackets()) {
                if (this.encode(ctx, packet)) {
                    passedThought++;
                } else {
                    encodedPackets++;
                }
            }
            this.recordMetrics(ctx, passedThought, encodedPackets);
        }
        out.add(msg.retain());
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, BedrockBatchWrapper msg, List<Object> out) throws Exception {
        for (BedrockPacketWrapper packet : msg.getPackets()) {
            this.decode(ctx, packet);
        }
        out.add(msg.retain());
    }

    protected final boolean encode(ChannelHandlerContext ctx, BedrockPacketWrapper msg) throws Exception {
        if (msg.getPacketBuffer() != null) {
            // We have a pre-encoded packet buffer, just use that.
            return true;
        }

        ByteBuf buf = ctx.alloc().buffer(128);
        try {
            BedrockPacket packet = msg.getPacket();
            msg.setPacketId(getPacketId(packet));
            this.encodeHeader(buf, msg);
            this.codec.tryEncode(helper, buf, packet);
            msg.setPacketBuffer(buf.retain());
        } catch (Throwable t) {
            log.error("Error encoding packet {}", msg.getPacket(), t);
        } finally {
            buf.release();
        }
        return false;
    }

    protected final void decode(ChannelHandlerContext ctx, BedrockPacketWrapper wrapper) throws Exception {
        if (wrapper.getPacketBuffer() == null) {
            throw new IllegalStateException("Packet has no encoded buffer");
        }

        ByteBuf msg = wrapper.getPacketBuffer().retainedSlice();
        try {
            int index = msg.readerIndex();
            this.decodeHeader(msg, wrapper);
            wrapper.setHeaderLength(msg.readerIndex() - index);
            wrapper.setPacket(this.codec.tryDecode(helper, msg, wrapper.getPacketId()));
        } catch (Throwable t) {
            log.info("Failed to decode packet", t);
            throw t;
        } finally {
            msg.release();
        }
    }

    public abstract void encodeHeader(ByteBuf buf, BedrockPacketWrapper msg);

    public abstract void decodeHeader(ByteBuf buf, BedrockPacketWrapper msg);

    private void recordMetrics(ChannelHandlerContext ctx, int passedThought, int encodedPackets) {
        NetworkMetrics metrics = ctx.channel().attr(NetworkMetrics.ATTRIBUTE).get();
        if (metrics == null) {
            return;
        }

        PacketDirection direction = ctx.channel().attr(PacketDirection.ATTRIBUTE).get();
        if (passedThought > 0) {
            metrics.passedThroughPackets(passedThought, direction);
        }

        if (encodedPackets > 0) {
            metrics.encodedPackets(passedThought, direction);
        }
    }

    public final int getPacketId(BedrockPacket packet) {
        if (packet instanceof UnknownPacket) {
            return ((UnknownPacket) packet).getPacketId();
        }
        return this.codec.getPacketDefinition(packet.getClass()).getId();
    }

    public final BedrockPacketCodec setCodec(BedrockCodec codec) {
        if (this.codec != BedrockCompat.CODEC) {
            throw new IllegalStateException("Codec is already set");
        }
        if (codec == BedrockCompat.CODEC) {
            throw new IllegalArgumentException("Cannot set codec to BedrockCompat");
        }
        this.codec = codec;
        this.helper = codec.createHelper();
        return this;
    }

    public final BedrockPacketCodec setCodecHelper(BedrockCodec codec, BedrockCodecHelper helper) {
        if (this.codec != BedrockCompat.CODEC) {
            throw new IllegalStateException("Codec is already set");
        }
        if (codec == BedrockCompat.CODEC) {
            throw new IllegalArgumentException("Cannot set codec to BedrockCompat");
        }
        this.codec = codec;
        this.helper = helper;
        return this;
    }

    public final BedrockCodec getCodec() {
        return codec;
    }

    public BedrockCodecHelper getHelper() {
        return helper;
    }
}
