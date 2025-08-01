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

package dev.waterdog.waterdogpe.network.protocol;

import dev.waterdog.waterdogpe.network.protocol.updaters.CodecUpdater419;
import dev.waterdog.waterdogpe.network.protocol.updaters.ProtocolCodecUpdater;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.packet.*;

import java.util.ArrayList;
import java.util.List;

public class ProtocolCodecs {
    
    private static final List<Class<? extends BedrockPacket>> HANDLED_PACKETS = new ArrayList<>();
    static {
        HANDLED_PACKETS.add(LoginPacket.class);
        HANDLED_PACKETS.add(PlayStatusPacket.class);
        HANDLED_PACKETS.add(ServerToClientHandshakePacket.class);
        HANDLED_PACKETS.add(ClientToServerHandshakePacket.class);
        HANDLED_PACKETS.add(DisconnectPacket.class);
        HANDLED_PACKETS.add(ResourcePacksInfoPacket.class);
        HANDLED_PACKETS.add(ResourcePackStackPacket.class);
        HANDLED_PACKETS.add(ResourcePackClientResponsePacket.class);
        HANDLED_PACKETS.add(ResourcePackDataInfoPacket.class);
        HANDLED_PACKETS.add(ResourcePackChunkDataPacket.class);
        HANDLED_PACKETS.add(ResourcePackChunkRequestPacket.class);
        HANDLED_PACKETS.add(TextPacket.class);
        HANDLED_PACKETS.add(StartGamePacket.class);
        HANDLED_PACKETS.add(AddPlayerPacket.class);
        HANDLED_PACKETS.add(AddEntityPacket.class);
        HANDLED_PACKETS.add(RemoveEntityPacket.class);
        HANDLED_PACKETS.add(AddItemEntityPacket.class);
        HANDLED_PACKETS.add(TakeItemEntityPacket.class);
        HANDLED_PACKETS.add(MoveEntityAbsolutePacket.class);
        HANDLED_PACKETS.add(MovePlayerPacket.class);
        HANDLED_PACKETS.add(UpdateBlockPacket.class);
        HANDLED_PACKETS.add(AddPaintingPacket.class);
        HANDLED_PACKETS.add(LevelEventPacket.class);
        HANDLED_PACKETS.add(EntityEventPacket.class);
        HANDLED_PACKETS.add(MobEffectPacket.class);
        HANDLED_PACKETS.add(UpdateAttributesPacket.class);
        HANDLED_PACKETS.add(MobEquipmentPacket.class);
        HANDLED_PACKETS.add(MobArmorEquipmentPacket.class);
        HANDLED_PACKETS.add(InteractPacket.class);
        HANDLED_PACKETS.add(EntityPickRequestPacket.class);
        HANDLED_PACKETS.add(PlayerActionPacket.class);
        HANDLED_PACKETS.add(SetEntityDataPacket.class);
        HANDLED_PACKETS.add(SetEntityMotionPacket.class);
        HANDLED_PACKETS.add(SetEntityLinkPacket.class);
        HANDLED_PACKETS.add(AnimatePacket.class);
        HANDLED_PACKETS.add(RespawnPacket.class);
        HANDLED_PACKETS.add(SetDifficultyPacket.class);
        HANDLED_PACKETS.add(ChangeDimensionPacket.class);
        HANDLED_PACKETS.add(SetPlayerGameTypePacket.class);
        HANDLED_PACKETS.add(PlayerListPacket.class);
        HANDLED_PACKETS.add(EventPacket.class);
        HANDLED_PACKETS.add(RequestChunkRadiusPacket.class);
        HANDLED_PACKETS.add(GameRulesChangedPacket.class);
        HANDLED_PACKETS.add(BossEventPacket.class);
        HANDLED_PACKETS.add(CommandRequestPacket.class);
        HANDLED_PACKETS.add(UpdateTradePacket.class);
        HANDLED_PACKETS.add(TransferPacket.class);
        HANDLED_PACKETS.add(StopSoundPacket.class);
        HANDLED_PACKETS.add(SetTitlePacket.class);
        HANDLED_PACKETS.add(NpcRequestPacket.class);
        HANDLED_PACKETS.add(RemoveObjectivePacket.class);
        HANDLED_PACKETS.add(SetDisplayObjectivePacket.class);
        HANDLED_PACKETS.add(SetScorePacket.class);
        HANDLED_PACKETS.add(MoveEntityDeltaPacket.class);
        HANDLED_PACKETS.add(SetScoreboardIdentityPacket.class);
        HANDLED_PACKETS.add(SetLocalPlayerAsInitializedPacket.class);
        HANDLED_PACKETS.add(NetworkStackLatencyPacket.class);
        HANDLED_PACKETS.add(SpawnParticleEffectPacket.class);
        HANDLED_PACKETS.add(AvailableEntityIdentifiersPacket.class);
        HANDLED_PACKETS.add(NetworkChunkPublisherUpdatePacket.class);
        HANDLED_PACKETS.add(LevelSoundEventPacket.class);
        HANDLED_PACKETS.add(ClientCacheStatusPacket.class);
        HANDLED_PACKETS.add(ClientCacheBlobStatusPacket.class);
        HANDLED_PACKETS.add(ClientCacheMissResponsePacket.class);
        HANDLED_PACKETS.add(EmotePacket.class);
        HANDLED_PACKETS.add(EmoteListPacket.class);
        HANDLED_PACKETS.add(DebugInfoPacket.class);
        HANDLED_PACKETS.add(PacketViolationWarningPacket.class);
        HANDLED_PACKETS.add(AnimateEntityPacket.class);
        HANDLED_PACKETS.add(ItemComponentPacket.class);
        HANDLED_PACKETS.add(NpcDialoguePacket.class);
        HANDLED_PACKETS.add(BiomeDefinitionListPacket.class);
        HANDLED_PACKETS.add(ChangeMobPropertyPacket.class);
        HANDLED_PACKETS.add(UpdateAbilitiesPacket.class);
        HANDLED_PACKETS.add(NetworkSettingsPacket.class);
        HANDLED_PACKETS.add(RequestNetworkSettingsPacket.class);
        HANDLED_PACKETS.add(UpdatePlayerGameTypePacket.class);
        HANDLED_PACKETS.add(SubClientLoginPacket.class);
        HANDLED_PACKETS.add(ToastRequestPacket.class);
        HANDLED_PACKETS.add(MovementEffectPacket.class);
        HANDLED_PACKETS.add(PlaySoundPacket.class);
        HANDLED_PACKETS.add(PlayerAuthInputPacket.class);
        HANDLED_PACKETS.add(ModalFormRequestPacket.class);
        HANDLED_PACKETS.add(ModalFormResponsePacket.class);
        HANDLED_PACKETS.add(BlockEntityDataPacket.class);
        HANDLED_PACKETS.add(InventoryTransactionPacket.class);
        HANDLED_PACKETS.add(ClientboundCloseFormPacket.class);
    }

    private static final List<ProtocolCodecUpdater> UPDATERS = new ObjectArrayList<>();
    private static final ProtocolCodecUpdater DEFAULT_UPDATER = (builder, codec) -> builder.retainPackets(HANDLED_PACKETS.toArray(new Class[]{}));
    static {
        UPDATERS.add(new CodecUpdater419());
    }

    public static void addUpdater(ProtocolCodecUpdater updater) {
        UPDATERS.add(updater);
    }

    public static List<ProtocolCodecUpdater> getUpdaters() {
        return UPDATERS;
    }

    public static BedrockCodec buildCodec(BedrockCodec baseCodec) {
        BedrockCodec.Builder builder = baseCodec.toBuilder();
        DEFAULT_UPDATER.updateCodec(builder, baseCodec);

        for (ProtocolCodecUpdater updater : UPDATERS) {
            if (baseCodec.getProtocolVersion() >= updater.getRequiredVersion()) {
                updater.updateCodec(builder, baseCodec);
            }
        }
        return builder.build();
    }
}
