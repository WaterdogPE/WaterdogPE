/**
 * Copyright 2020 WaterdogTEAM
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pe.waterdog.network.protocol.codec;

import com.nukkitx.protocol.bedrock.BedrockPacketCodec;
import com.nukkitx.protocol.bedrock.packet.*;
import com.nukkitx.protocol.bedrock.v419.BedrockPacketHelper_v419;
import com.nukkitx.protocol.bedrock.v419.serializer.*;
import pe.waterdog.network.protocol.ProtocolVersion;

public class BedrockCodec419 extends BedrockCodec408 {

    @Override
    public ProtocolVersion getProtocol() {
        return ProtocolVersion.MINECRAFT_PE_1_16_100;
    }

    @Override
    public void buildCodec(BedrockPacketCodec.Builder builder) {
        super.buildCodec(builder);
        builder.helper(BedrockPacketHelper_v419.INSTANCE);

        builder.deregisterPacket(ResourcePackStackPacket.class);
        builder.registerPacket(ResourcePackStackPacket.class, ResourcePackStackSerializer_v419.INSTANCE, 7);

        builder.deregisterPacket(StartGamePacket.class);
        builder.registerPacket(StartGamePacket.class, StartGameSerializer_v419.INSTANCE, 11);

        builder.deregisterPacket(MovePlayerPacket.class);
        builder.registerPacket(MovePlayerPacket.class, MovePlayerSerializer_v419.INSTANCE, 19);

        builder.deregisterPacket(UpdateAttributesPacket.class);
        builder.registerPacket(UpdateAttributesPacket.class, UpdateAttributesSerializer_v419.INSTANCE, 29);

        builder.deregisterPacket(SetEntityDataPacket.class);
        builder.registerPacket(SetEntityDataPacket.class, SetEntityDataSerializer_v419.INSTANCE, 39);

        builder.deregisterPacket(MoveEntityDeltaPacket.class);
        builder.registerPacket(MoveEntityDeltaPacket.class, MoveEntityDeltaSerializer_v419.INSTANCE, 111);
    }
}
