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

import io.netty.buffer.ByteBuf;
import org.cloudburstmc.protocol.bedrock.netty.BedrockPacketWrapper;
import org.cloudburstmc.protocol.common.util.VarInts;

public class BedrockPacketCodec_v3 extends BedrockPacketCodec {

    @Override
    public void encodeHeader(ByteBuf buf, BedrockPacketWrapper msg) {
        int header = 0;
        header |= (msg.getPacketId() & 0x3ff);
        header |= (msg.getSenderSubClientId() & 3) << 10;
        header |= (msg.getTargetSubClientId() & 3) << 12;
        VarInts.writeUnsignedInt(buf, header);
    }

    @Override
    public void decodeHeader(ByteBuf buf, BedrockPacketWrapper msg) {
        int header = VarInts.readUnsignedInt(buf);
        msg.setPacketId(header & 0x3ff);
        msg.setSenderSubClientId((header >> 10) & 3);
        msg.setTargetSubClientId((header >> 12) & 3);
    }
}
