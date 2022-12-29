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

import dev.waterdog.waterdogpe.network.connection.codec.BedrockBatchWrapper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;

import java.util.List;

public class NoopCompressionCodec extends MessageToMessageCodec<BedrockBatchWrapper, BedrockBatchWrapper> {
    @Override
    protected void encode(ChannelHandlerContext ctx, BedrockBatchWrapper msg, List<Object> out) throws Exception {
        msg.setCompressed(msg.getUncompressed().retainedSlice(), null);
        out.add(msg.retain());
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, BedrockBatchWrapper msg, List<Object> out) throws Exception {
        msg.setAlgorithm(null);
        msg.setUncompressed(msg.getCompressed().retainedSlice());
        out.add(msg.retain());
    }
}
