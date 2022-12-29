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

package dev.waterdog.waterdogpe.network.connection.codec.encryption;

import dev.waterdog.waterdogpe.network.connection.codec.BedrockBatchWrapper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.MessageToMessageDecoder;
import lombok.RequiredArgsConstructor;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static dev.waterdog.waterdogpe.network.connection.codec.encryption.BedrockEncryptionEncoder.generateTrailer;

@RequiredArgsConstructor
public class BedrockEncryptionDecoder extends MessageToMessageDecoder<BedrockBatchWrapper> {

    public static final String NAME = "bedrock-encryption-decoder";

    private static final boolean VALIDATE = Boolean.getBoolean("cloudburst.validateEncryption");

    private final AtomicLong packetCounter = new AtomicLong();
    private final SecretKey key;
    private final Cipher cipher;

    @Override
    protected void decode(ChannelHandlerContext ctx, BedrockBatchWrapper msg, List<Object> out) throws Exception {
        ByteBuffer inBuffer = msg.getCompressed().nioBuffer();
        ByteBuffer outBuffer = inBuffer.duplicate();

        // Copy-safe so we can use the same buffer.
        this.cipher.update(inBuffer, outBuffer);

        ByteBuf output = msg.getCompressed().readSlice(msg.getCompressed().readableBytes() - 8);

        if (VALIDATE) {
            ByteBuf trailer = msg.getCompressed().readSlice(8);

            byte[] actual = new byte[8];
            trailer.readBytes(actual);

            byte[] expected = generateTrailer(output, this.key, this.packetCounter);

            if (!Arrays.equals(expected, actual)) {
                throw new CorruptedFrameException("Invalid encryption trailer");
            }
        }

        msg.setCompressed(output.retain());
        out.add(msg.retain());
    }
}
