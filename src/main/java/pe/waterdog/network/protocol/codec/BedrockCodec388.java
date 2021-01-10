/*
 * Copyright 2020 WaterdogTEAM
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package pe.waterdog.network.protocol.codec;

import com.nukkitx.protocol.bedrock.BedrockPacketCodec;
import com.nukkitx.protocol.bedrock.packet.*;
import com.nukkitx.protocol.bedrock.v291.serializer.*;
import com.nukkitx.protocol.bedrock.v313.serializer.AddEntitySerializer_v313;
import com.nukkitx.protocol.bedrock.v313.serializer.LevelSoundEvent2Serializer_v313;
import com.nukkitx.protocol.bedrock.v313.serializer.NetworkChunkPublisherUpdateSerializer_v313;
import com.nukkitx.protocol.bedrock.v332.serializer.LevelSoundEventSerializer_v332;
import com.nukkitx.protocol.bedrock.v332.serializer.NetworkStackLatencySerializer_v332;
import com.nukkitx.protocol.bedrock.v332.serializer.ResourcePacksInfoSerializer_v332;
import com.nukkitx.protocol.bedrock.v332.serializer.TextSerializer_v332;
import com.nukkitx.protocol.bedrock.v354.serializer.UpdateTradeSerializer_v354;
import com.nukkitx.protocol.bedrock.v361.serializer.AddPaintingSerializer_v361;
import com.nukkitx.protocol.bedrock.v361.serializer.LevelChunkSerializer_v361;
import com.nukkitx.protocol.bedrock.v361.serializer.ResourcePackDataInfoSerializer_v361;
import com.nukkitx.protocol.bedrock.v388.BedrockPacketHelper_v388;
import com.nukkitx.protocol.bedrock.v388.serializer.*;
import pe.waterdog.network.protocol.ProtocolVersion;

public class BedrockCodec388 extends BedrockCodec {

    @Override
    public ProtocolVersion getProtocol() {
        return ProtocolVersion.MINECRAFT_PE_1_13;
    }

    @Override
    public void buildCodec(BedrockPacketCodec.Builder builder) {
        super.buildCodec(builder);
        builder.helper(BedrockPacketHelper_v388.INSTANCE);

        builder.registerPacket(LoginPacket.class, LoginSerializer_v291.INSTANCE, 1);
        builder.registerPacket(PlayStatusPacket.class, PlayStatusSerializer_v291.INSTANCE, 2);
        builder.registerPacket(ServerToClientHandshakePacket.class, ServerToClientHandshakeSerializer_v291.INSTANCE, 3);
        builder.registerPacket(ClientToServerHandshakePacket.class, ClientToServerHandshakeSerializer_v291.INSTANCE, 4);
        builder.registerPacket(DisconnectPacket.class, DisconnectSerializer_v291.INSTANCE, 5);

        builder.registerPacket(ResourcePacksInfoPacket.class, ResourcePacksInfoSerializer_v332.INSTANCE, 6);
        builder.registerPacket(ResourcePackStackPacket.class, ResourcePackStackSerializer_v388.INSTANCE, 7);
        builder.registerPacket(ResourcePackClientResponsePacket.class, ResourcePackClientResponseSerializer_v291.INSTANCE, 8);
        builder.registerPacket(ResourcePackDataInfoPacket.class, ResourcePackDataInfoSerializer_v361.INSTANCE, 82);
        builder.registerPacket(ResourcePackChunkDataPacket.class, ResourcePackChunkDataSerializer_v388.INSTANCE, 83);
        builder.registerPacket(ResourcePackChunkRequestPacket.class, ResourcePackChunkRequestSerializer_v291.INSTANCE, 84);

        builder.registerPacket(TextPacket.class, TextSerializer_v332.INSTANCE, 9);
        builder.registerPacket(StartGamePacket.class, StartGameSerializer_v388.INSTANCE, 11);
        builder.registerPacket(AddPlayerPacket.class, AddPlayerSerializer_v388.INSTANCE, 12);
        builder.registerPacket(AddEntityPacket.class, AddEntitySerializer_v313.INSTANCE, 13);
        builder.registerPacket(RemoveEntityPacket.class, RemoveEntitySerializer_v291.INSTANCE, 14);
        builder.registerPacket(AddItemEntityPacket.class, AddItemEntitySerializer_v291.INSTANCE, 15);
        builder.registerPacket(TakeItemEntityPacket.class, TakeItemEntitySerializer_v291.INSTANCE, 17);
        builder.registerPacket(MoveEntityAbsolutePacket.class, MoveEntityAbsoluteSerializer_v291.INSTANCE, 18);
        builder.registerPacket(MovePlayerPacket.class, MovePlayerSerializer_v291.INSTANCE, 19);
        builder.registerPacket(UpdateBlockPacket.class, UpdateBlockSerializer_v291.INSTANCE, 21);
        builder.registerPacket(AddPaintingPacket.class, AddPaintingSerializer_v361.INSTANCE, 22);
        builder.registerPacket(TickSyncPacket.class, TickSyncSerializer_v388.INSTANCE, 23);
        builder.registerPacket(LevelSoundEvent1Packet.class, LevelSoundEvent1Serializer_v291.INSTANCE, 24);
        builder.registerPacket(LevelEventPacket.class, LevelEventSerializer_v291.INSTANCE, 25);
        builder.registerPacket(EntityEventPacket.class, EntityEventSerializer_v291.INSTANCE, 27);
        builder.registerPacket(MobEffectPacket.class, MobEffectSerializer_v291.INSTANCE, 28);
        builder.registerPacket(UpdateAttributesPacket.class, UpdateAttributesSerializer_v291.INSTANCE, 29);
        builder.registerPacket(MobEquipmentPacket.class, MobEquipmentSerializer_v291.INSTANCE, 31);
        builder.registerPacket(MobArmorEquipmentPacket.class, MobArmorEquipmentSerializer_v291.INSTANCE, 32);
        builder.registerPacket(InteractPacket.class, InteractSerializer_v388.INSTANCE, 33);
        builder.registerPacket(PlayerActionPacket.class, PlayerActionSerializer_v291.INSTANCE, 36);
        builder.registerPacket(SetEntityDataPacket.class, SetEntityDataSerializer_v291.INSTANCE, 39);
        builder.registerPacket(SetEntityMotionPacket.class, SetEntityMotionSerializer_v291.INSTANCE, 40);
        builder.registerPacket(SetEntityLinkPacket.class, SetEntityLinkSerializer_v291.INSTANCE, 41);
        builder.registerPacket(AnimatePacket.class, AnimateSerializer_v291.INSTANCE, 44);
        builder.registerPacket(RespawnPacket.class, RespawnSerializer_v388.INSTANCE, 45);
        builder.registerPacket(AdventureSettingsPacket.class, AdventureSettingsSerializer_v291.INSTANCE, 55);
        builder.registerPacket(LevelChunkPacket.class, LevelChunkSerializer_v361.INSTANCE, 58);
        builder.registerPacket(SetDifficultyPacket.class, SetDifficultySerializer_v291.INSTANCE, 60);
        builder.registerPacket(SetPlayerGameTypePacket.class, SetPlayerGameTypeSerializer_v291.INSTANCE, 62);
        builder.registerPacket(PlayerListPacket.class, PlayerListSerializer_v388.INSTANCE, 63);
        builder.registerPacket(RequestChunkRadiusPacket.class, RequestChunkRadiusSerializer_v291.INSTANCE, 69);
        builder.registerPacket(GameRulesChangedPacket.class, GameRulesChangedSerializer_v291.INSTANCE, 72);
        builder.registerPacket(BossEventPacket.class, BossEventSerializer_v291.INSTANCE, 74);
        builder.registerPacket(CommandRequestPacket.class, CommandRequestSerializer_v291.INSTANCE, 77);
        builder.registerPacket(UpdateTradePacket.class, UpdateTradeSerializer_v354.INSTANCE, 80);
        builder.registerPacket(TransferPacket.class, TransferSerializer_v291.INSTANCE, 85);
        builder.registerPacket(SetTitlePacket.class, SetTitleSerializer_v291.INSTANCE, 88);
        builder.registerPacket(RemoveObjectivePacket.class, RemoveObjectiveSerializer_v291.INSTANCE, 106);
        builder.registerPacket(SetDisplayObjectivePacket.class, SetDisplayObjectiveSerializer_v291.INSTANCE, 107);
        builder.registerPacket(SetScorePacket.class, SetScoreSerializer_v291.INSTANCE, 108);
        builder.registerPacket(MoveEntityDeltaPacket.class, MoveEntityDeltaSerializer_v388.INSTANCE, 111);
        builder.registerPacket(SetScoreboardIdentityPacket.class, SetScoreboardIdentitySerializer_v291.INSTANCE, 112);
        builder.registerPacket(SetLocalPlayerAsInitializedPacket.class, SetLocalPlayerAsInitializedSerializer_v291.INSTANCE, 113);
        builder.registerPacket(NetworkStackLatencyPacket.class, NetworkStackLatencySerializer_v332.INSTANCE, 115);
        builder.registerPacket(LevelSoundEvent2Packet.class, LevelSoundEvent2Serializer_v313.INSTANCE, 120);
        builder.registerPacket(NetworkChunkPublisherUpdatePacket.class, NetworkChunkPublisherUpdateSerializer_v313.INSTANCE, 121);
        builder.registerPacket(LevelSoundEventPacket.class, LevelSoundEventSerializer_v332.INSTANCE, 123);
        builder.registerPacket(EmotePacket.class, EmoteSerializer_v388.INSTANCE, 138);
    }

    @Override
    public void registerCommands(BedrockPacketCodec.Builder builder) {
        builder.registerPacket(AvailableCommandsPacket.class, AvailableCommandsSerializer_v388.INSTANCE, 76);
    }
}
