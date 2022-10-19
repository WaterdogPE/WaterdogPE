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
import com.nukkitx.protocol.bedrock.packet.SetEntityDataPacket;
import com.nukkitx.protocol.bedrock.v557.BedrockPacketHelper_v557;
import com.nukkitx.protocol.bedrock.v557.serializer.AddEntitySerializer_v557;
import com.nukkitx.protocol.bedrock.v557.serializer.AddPlayerSerializer_v557;
import com.nukkitx.protocol.bedrock.v557.serializer.SetEntityDataSerializer_v557;
import dev.waterdog.waterdogpe.network.protocol.ProtocolVersion;

/**
 * @author Kaooot
 * @version 1.0
 */
public class BedrockCodec557 extends BedrockCodec554 {

    @Override
    public ProtocolVersion getProtocol() {
        return ProtocolVersion.MINECRAFT_PE_1_19_40;
    }

    @Override
    public void buildCodec(BedrockPacketCodec.Builder builder) {
        super.buildCodec(builder);
        builder.helper(BedrockPacketHelper_v557.INSTANCE);

        builder.deregisterPacket(AddPlayerPacket.class);
        builder.registerPacket(AddPlayerPacket.class, AddPlayerSerializer_v557.INSTANCE, 12);

        builder.deregisterPacket(AddEntityPacket.class);
        builder.registerPacket(AddEntityPacket.class, AddEntitySerializer_v557.INSTANCE, 13);

        builder.deregisterPacket(SetEntityDataPacket.class);
        builder.registerPacket(SetEntityDataPacket.class, SetEntityDataSerializer_v557.INSTANCE, 39);
    }
}