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

package dev.waterdog.network.protocol.codec;

import com.nukkitx.protocol.bedrock.BedrockPacketCodec;
import com.nukkitx.protocol.bedrock.packet.*;
import com.nukkitx.protocol.bedrock.v388.BedrockPacketHelper_v388;
import com.nukkitx.protocol.bedrock.v388.serializer.*;
import dev.waterdog.network.protocol.ProtocolVersion;

public class BedrockCodec388 extends BedrockCodec361 {

    @Override
    public ProtocolVersion getProtocol() {
        return ProtocolVersion.MINECRAFT_PE_1_13;
    }

    @Override
    public void buildCodec(BedrockPacketCodec.Builder builder) {
        super.buildCodec(builder);
        builder.helper(BedrockPacketHelper_v388.INSTANCE);

        builder.deregisterPacket(ResourcePackStackPacket.class);
        builder.registerPacket(ResourcePackStackPacket.class, ResourcePackStackSerializer_v388.INSTANCE, 7);

        builder.deregisterPacket(ResourcePackChunkDataPacket.class);
        builder.registerPacket(ResourcePackChunkDataPacket.class, ResourcePackChunkDataSerializer_v388.INSTANCE, 83);

        builder.deregisterPacket(StartGamePacket.class);
        builder.registerPacket(StartGamePacket.class, StartGameSerializer_v388.INSTANCE, 11);

        builder.deregisterPacket(AddPlayerPacket.class);
        builder.registerPacket(AddPlayerPacket.class, AddPlayerSerializer_v388.INSTANCE, 12);

        builder.registerPacket(TickSyncPacket.class, TickSyncSerializer_v388.INSTANCE, 23);

        builder.deregisterPacket(InteractPacket.class);
        builder.registerPacket(InteractPacket.class, InteractSerializer_v388.INSTANCE, 33);

        builder.deregisterPacket(RespawnPacket.class);
        builder.registerPacket(RespawnPacket.class, RespawnSerializer_v388.INSTANCE, 45);

        builder.deregisterPacket(PlayerListPacket.class);
        builder.registerPacket(PlayerListPacket.class, PlayerListSerializer_v388.INSTANCE, 63);

        builder.deregisterPacket(MoveEntityDeltaPacket.class);
        builder.registerPacket(MoveEntityDeltaPacket.class, MoveEntityDeltaSerializer_v388.INSTANCE, 111);

        builder.registerPacket(EmotePacket.class, EmoteSerializer_v388.INSTANCE, 138);
    }

    @Override
    public void registerCommands(BedrockPacketCodec.Builder builder) {
        builder.registerPacket(AvailableCommandsPacket.class, AvailableCommandsSerializer_v388.INSTANCE, 76);
    }
}
