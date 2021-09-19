/*
 * Copyright 2021 WaterdogTEAM
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

package dev.waterdog.waterdogpe.network.protocol.codec;

import com.nukkitx.protocol.bedrock.BedrockPacketCodec;
import com.nukkitx.protocol.bedrock.packet.EntityPickRequestPacket;
import com.nukkitx.protocol.bedrock.packet.StartGamePacket;
import com.nukkitx.protocol.bedrock.v448.BedrockPacketHelper_v465;
import com.nukkitx.protocol.bedrock.v448.serializer.EntityPickRequestSerializer_v465;
import com.nukkitx.protocol.bedrock.v448.serializer.StartGameSerializer_v465;
import dev.waterdog.waterdogpe.network.protocol.ProtocolVersion;

public class BedrockCodec465 extends BedrockCodec448 {

    @Override
    public ProtocolVersion getProtocol() {
        return ProtocolVersion.MINECRAFT_PE_1_17_30;
    }

    @Override
    public void buildCodec(BedrockPacketCodec.Builder builder) {
        super.buildCodec(builder);
        builder.helper(BedrockPacketHelper_v465.INSTANCE);

        builder.deregisterPacket(StartGamePacket.class);
        builder.registerPacket(StartGamePacket.class, StartGameSerializer_v465.INSTANCE, 11);

        builder.deregisterPacket(EntityPickRequestPacket.class);
        builder.registerPacket(EntityPickRequestPacket.class, EntityPickRequestSerializer_v465.INSTANCE, 35);
    }
}
