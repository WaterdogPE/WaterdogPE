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

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.cloudburstmc.protocol.common.util.Zlib;

@RequiredArgsConstructor
public class ZlibCompressionCodec extends ProxiedCompressionCodec {
    private static final int MAX_DECOMPRESSED_BYTES = 1024 * 1024 * 12;

    private final Zlib zlib;

    @Getter @Setter
    private int level = 7;

    @Override
    public ByteBuf encode0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        ByteBuf outBuf = ctx.alloc().ioBuffer(msg.readableBytes() << 3);
        try {
            zlib.deflate(msg, outBuf, level);
            return outBuf.retain();
        } finally {
            outBuf.release();
        }
    }

    @Override
    public ByteBuf decode0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        return zlib.inflate(msg, MAX_DECOMPRESSED_BYTES);
    }

    @Override
    public CompressionAlgorithm getCompressionAlgorithm() {
        return CompressionAlgorithm.ZLIB;
    }
}
