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

package dev.waterdog.waterdogpe.network.connection.codec.compression;

import dev.waterdog.waterdogpe.network.NetworkMetrics;
import dev.waterdog.waterdogpe.network.PacketDirection;
import dev.waterdog.waterdogpe.network.connection.codec.BedrockBatchWrapper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import org.cloudburstmc.protocol.bedrock.data.PacketCompressionAlgorithm;
import org.cloudburstmc.protocol.bedrock.netty.codec.compression.CompressionCodec;

import java.util.List;

public abstract class ProxiedCompressionCodec extends MessageToMessageCodec<BedrockBatchWrapper, BedrockBatchWrapper> implements CompressionCodec {
    public static final String NAME = "compression-codec";

    @Override
    public void encode(ChannelHandlerContext ctx, BedrockBatchWrapper msg, List<Object> out) throws Exception {
        if (msg.getCompressed() == null && msg.getUncompressed() == null) {
            throw new IllegalStateException("Batch was not encoded before");
        }

        NetworkMetrics metrics = ctx.channel().attr(NetworkMetrics.ATTRIBUTE).get();
        PacketDirection direction = ctx.channel().attr(PacketDirection.ATTRIBUTE).get();

        if (msg.getCompressed() == null || msg.isModified()) {
            msg.setCompressed(this.encode0(ctx, msg.getUncompressed()), this.getCompressionAlgorithm());
            if (metrics != null) metrics.compressedBytes(msg.getCompressed().readableBytes(), direction);
        } else if (metrics != null) {
            metrics.passedThroughBytes(msg.getCompressed().readableBytes(), direction);
        }

        out.add(msg.retain());
    }

    @Override
    public void decode(ChannelHandlerContext ctx, BedrockBatchWrapper msg, List<Object> out) throws Exception {
        try {
            msg.setAlgorithm(this.getCompressionAlgorithm());
            msg.setUncompressed(this.decode0(ctx, msg.getCompressed().slice()));

            NetworkMetrics metrics = ctx.channel().attr(NetworkMetrics.ATTRIBUTE).get();
            if (metrics != null) {
                PacketDirection direction = ctx.channel().attr(PacketDirection.ATTRIBUTE).get();
                metrics.decompressedBytes(msg.getUncompressed().readableBytes(), direction);
            }

            out.add(msg.retain());
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    protected abstract ByteBuf encode0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception;
    protected abstract ByteBuf decode0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception;

    public abstract CompressionAlgorithm getCompressionAlgorithm();

    @Override
    public final PacketCompressionAlgorithm getAlgorithm() {
        return this.getCompressionAlgorithm().getBedrockAlgorithm();
    }
}
