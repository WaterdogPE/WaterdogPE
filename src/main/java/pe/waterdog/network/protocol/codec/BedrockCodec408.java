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
import com.nukkitx.protocol.bedrock.v291.serializer.*;
import com.nukkitx.protocol.bedrock.v313.serializer.AddEntitySerializer_v313;
import com.nukkitx.protocol.bedrock.v313.serializer.NetworkChunkPublisherUpdateSerializer_v313;
import com.nukkitx.protocol.bedrock.v332.serializer.NetworkStackLatencySerializer_v332;
import com.nukkitx.protocol.bedrock.v332.serializer.ResourcePacksInfoSerializer_v332;
import com.nukkitx.protocol.bedrock.v332.serializer.TextSerializer_v332;
import com.nukkitx.protocol.bedrock.v354.serializer.*;
import com.nukkitx.protocol.bedrock.v361.serializer.*;
import com.nukkitx.protocol.bedrock.v388.serializer.*;
import com.nukkitx.protocol.bedrock.v390.serializer.PlayerListSerializer_v390;
import com.nukkitx.protocol.bedrock.v407.BedrockPacketHelper_v407;
import com.nukkitx.protocol.bedrock.v407.serializer.*;
import pe.waterdog.network.protocol.ProtocolVersion;

public class BedrockCodec408 extends BedrockCodec390 {

    @Override
    public ProtocolVersion getProtocol() {
        return ProtocolVersion.MINECRAFT_PE_1_16_20;
    }

    @Override
    public void buildCodec(BedrockPacketCodec.Builder builder) {
        super.buildCodec(builder);
        builder.helper(BedrockPacketHelper_v407.INSTANCE);

        builder.deregisterPacket(StartGamePacket.class);
        builder.registerPacket(StartGamePacket.class, StartGameSerializer_v407.INSTANCE, 11);

        builder.deregisterPacket(LevelSoundEvent2Packet.class);
        builder.registerPacket(LevelSoundEvent2Packet.class, LevelSoundEvent2Serializer_v407.INSTANCE, 120);

        builder.deregisterPacket(LevelSoundEventPacket.class);
        builder.registerPacket(LevelSoundEventPacket.class, LevelSoundEventSerializer_v407.INSTANCE, 123);

        builder.deregisterPacket(LevelSoundEvent2Packet.class);
        builder.registerPacket(LevelSoundEvent2Packet.class, LevelSoundEvent2Serializer_v407.INSTANCE, 120);

        builder.registerPacket(DebugInfoPacket.class, DebugInfoSerializer_v407.INSTANCE, 155);
        builder.registerPacket(PacketViolationWarningPacket.class, PacketViolationWarningSerializer_v407.INSTANCE, 156);
    }
}
