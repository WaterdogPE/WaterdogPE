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

import io.airlift.compress.snappy.SnappyRawCompressor;
import io.airlift.compress.snappy.SnappyRawDecompressor;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelHandlerContext;
import lombok.RequiredArgsConstructor;

import static sun.misc.Unsafe.ARRAY_BYTE_BASE_OFFSET;

@RequiredArgsConstructor
public class SnappyCompressionCodec extends ProxiedCompressionCodec {

    private static final ThreadLocal<short[]> TABLE = ThreadLocal.withInitial(() -> new short[16384]);

    @Override
    public ByteBuf encode0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        ByteBuf direct;
        if (!msg.isDirect() || msg instanceof CompositeByteBuf) {
            direct = ctx.alloc().ioBuffer(msg.readableBytes());
            direct.writeBytes(msg);
        } else {
            direct = msg;
        }

        ByteBuf output = ctx.alloc().directBuffer();
        try {
            long inputAddress = direct.memoryAddress() + direct.readerIndex();
            long inputEndAddress = inputAddress + direct.readableBytes();

            output.ensureWritable(SnappyRawCompressor.maxCompressedLength(direct.readableBytes()));

            long outputAddress;
            long outputEndAddress;
            byte[] outputArray = null;
            if (output.isDirect() && output.hasMemoryAddress()) {
                outputAddress = output.memoryAddress() + output.writerIndex();
                outputEndAddress = outputAddress + output.writableBytes();
            } else if (output.hasArray()) {
                outputArray = output.array();
                outputAddress = ARRAY_BYTE_BASE_OFFSET + output.arrayOffset() + output.writerIndex();
                outputEndAddress = ARRAY_BYTE_BASE_OFFSET + output.arrayOffset() + output.writableBytes();
            } else {
                throw new IllegalStateException("Unsupported ByteBuf " + output.getClass().getSimpleName());
            }

            int compressed = SnappyRawCompressor.compress(null, inputAddress, inputEndAddress, outputArray, outputAddress, outputEndAddress, TABLE.get());
            output.writerIndex(output.writerIndex() + compressed);
            return output.retain();
        } finally {
            output.release();
            if (direct != msg) {
                direct.release();
            }
        }
    }

    @Override
    public ByteBuf decode0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        ByteBuf direct;
        if (!msg.isDirect() || msg instanceof CompositeByteBuf) {
            direct = ctx.alloc().ioBuffer(msg.readableBytes());
            direct.writeBytes(msg);
        } else {
            direct = msg;
        }

        ByteBuf output = ctx.alloc().directBuffer();
        try {
            long inputAddress = direct.memoryAddress() + direct.readerIndex();
            long inputEndAddress = inputAddress + direct.readableBytes();
            output.ensureWritable(SnappyRawDecompressor.getUncompressedLength(null, inputAddress, inputEndAddress));

            long outputAddress;
            long outputEndAddress;
            byte[] outputArray = null;
            if (output.isDirect() && output.hasMemoryAddress()) {
                outputAddress = output.memoryAddress() + output.writerIndex();
                outputEndAddress = outputAddress + output.writableBytes();
            } else if (output.hasArray()) {
                outputArray = output.array();
                outputAddress = ARRAY_BYTE_BASE_OFFSET + output.arrayOffset() + output.writerIndex();
                outputEndAddress = ARRAY_BYTE_BASE_OFFSET + output.arrayOffset() + output.writableBytes();
            } else {
                throw new IllegalStateException("Unsupported ByteBuf " + output.getClass().getSimpleName());
            }

            int decompressed = SnappyRawDecompressor.decompress(null, inputAddress, inputEndAddress, outputArray, outputAddress, outputEndAddress);
            output.writerIndex(output.writerIndex() + decompressed);
            return output.retain();
        } finally {
            output.release();
            if (direct != msg) {
                direct.release();
            }
        }
    }

    @Override
    public int getLevel() {
        return -1;
    }

    @Override
    public void setLevel(int level) {
        // no-op
    }

    @Override
    public CompressionAlgorithm getCompressionAlgorithm() {
        return CompressionAlgorithm.SNAPPY;
    }
}
