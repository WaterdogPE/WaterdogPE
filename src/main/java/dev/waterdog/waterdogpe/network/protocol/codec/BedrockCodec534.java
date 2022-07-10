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

package dev.waterdog.waterdogpe.network.protocol.codec;

import com.nukkitx.protocol.bedrock.BedrockPacketCodec;
import com.nukkitx.protocol.bedrock.packet.AddEntityPacket;
import com.nukkitx.protocol.bedrock.packet.AddPlayerPacket;
import com.nukkitx.protocol.bedrock.packet.StartGamePacket;
import com.nukkitx.protocol.bedrock.packet.UpdateAbilitiesPacket;
import com.nukkitx.protocol.bedrock.v534.BedrockPacketHelper_v534;
import com.nukkitx.protocol.bedrock.v534.serializer.AddEntitySerializer_v534;
import com.nukkitx.protocol.bedrock.v534.serializer.AddPlayerSerializer_v534;
import com.nukkitx.protocol.bedrock.v534.serializer.StartGameSerializer_v534;
import com.nukkitx.protocol.bedrock.v534.serializer.UpdateAbilitiesSerializer_v534;
import dev.waterdog.waterdogpe.network.protocol.ProtocolVersion;

/**
 * @author Kaooot
 * @version 1.0
 */
public class BedrockCodec534 extends BedrockCodec527 {

    @Override
    public ProtocolVersion getProtocol() {
        return ProtocolVersion.MINECRAFT_PE_1_19_10;
    }

    @Override
    public void buildCodec(BedrockPacketCodec.Builder builder) {
        super.buildCodec(builder);
        builder.helper(BedrockPacketHelper_v534.INSTANCE);

        builder.deregisterPacket(StartGamePacket.class);
        builder.registerPacket(StartGamePacket.class, StartGameSerializer_v534.INSTANCE, 11);

        builder.deregisterPacket(AddPlayerPacket.class);
        builder.registerPacket(AddPlayerPacket.class, AddPlayerSerializer_v534.INSTANCE, 12);

        builder.deregisterPacket(AddEntityPacket.class);
        builder.registerPacket(AddEntityPacket.class, AddEntitySerializer_v534.INSTANCE, 13);

        builder.registerPacket(UpdateAbilitiesPacket.class, UpdateAbilitiesSerializer_v534.INSTANCE, 187);
    }
}