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

package dev.waterdog.waterdogpe.network.protocol.rewrite;

import it.unimi.dsi.fastutil.longs.LongListIterator;
import org.cloudburstmc.protocol.bedrock.data.camera.CameraAttachToEntityInstruction;
import org.cloudburstmc.protocol.bedrock.data.debugshape.DebugShape;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataMap;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataType;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityLinkData;
import org.cloudburstmc.protocol.bedrock.packet.*;
import dev.waterdog.waterdogpe.network.protocol.rewrite.types.RewriteData;
import dev.waterdog.waterdogpe.network.protocol.user.PlayerRewriteUtils;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import org.cloudburstmc.protocol.common.PacketSignal;

import java.util.Arrays;
import java.util.Collection;
import java.util.ListIterator;

import static dev.waterdog.waterdogpe.network.protocol.Signals.mergeSignals;

/**
 * Class to map the proper entityIds to entity-related packets.
 */
public class EntityMap implements BedrockPacketHandler {
    private static final Collection<EntityDataType<Long>> ENTITY_DATA_FIELDS = Arrays.asList(
            EntityDataTypes.OWNER_EID,
            EntityDataTypes.TARGET_EID,
            EntityDataTypes.LEASH_HOLDER,
            EntityDataTypes.WITHER_TARGET_A,
            EntityDataTypes.WITHER_TARGET_B,
            EntityDataTypes.WITHER_TARGET_C,
            EntityDataTypes.TRADE_TARGET_EID,
            EntityDataTypes.BALLOON_ANCHOR_EID,
            EntityDataTypes.AGENT_EID
    );

    private final ProxiedPlayer player;
    private final RewriteData data;

    public EntityMap(ProxiedPlayer player) {
        this.player = player;
        this.data = player.getRewriteData();
    }

    public PacketSignal doRewrite(BedrockPacket packet) {
        return this.player.canRewrite() ? packet.handle(this) : PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(MoveEntityAbsolutePacket packet) {
        return data.rewriteEntityId(packet.getRuntimeEntityId(), packet::setRuntimeEntityId);
    }

    @Override
    public PacketSignal handle(EntityEventPacket packet) {
        return data.rewriteEntityId(packet.getRuntimeEntityId(), packet::setRuntimeEntityId);
    }

    @Override
    public PacketSignal handle(MobEffectPacket packet) {
        return data.rewriteEntityId(packet.getRuntimeEntityId(), packet::setRuntimeEntityId);
    }

    @Override
    public PacketSignal handle(UpdateAttributesPacket packet) {
        return data.rewriteEntityId(packet.getRuntimeEntityId(), packet::setRuntimeEntityId);
    }

    @Override
    public PacketSignal handle(MobEquipmentPacket packet) {
        return data.rewriteEntityId(packet.getRuntimeEntityId(), packet::setRuntimeEntityId);
    }

    @Override
    public PacketSignal handle(MobArmorEquipmentPacket packet) {
        return data.rewriteEntityId(packet.getRuntimeEntityId(), packet::setRuntimeEntityId);
    }

    @Override
    public PacketSignal handle(PlayerActionPacket packet) {
        return data.rewriteEntityId(packet.getRuntimeEntityId(), packet::setRuntimeEntityId);
    }

    @Override
    public PacketSignal handle(SetEntityDataPacket packet) {
        PacketSignal signal = data.rewriteEntityId(packet.getRuntimeEntityId(), packet::setRuntimeEntityId);
        PacketSignal metaSignal = this.rewriteMetadata(packet.getMetadata());
        return mergeSignals(signal, metaSignal);
    }

    @Override
    public PacketSignal handle(SetEntityMotionPacket packet) {
        return data.rewriteEntityId(packet.getRuntimeEntityId(), packet::setRuntimeEntityId);
    }

    @Override
    public PacketSignal handle(MoveEntityDeltaPacket packet) {
        return data.rewriteEntityId(packet.getRuntimeEntityId(), packet::setRuntimeEntityId);
    }

    @Override
    public PacketSignal handle(SetLocalPlayerAsInitializedPacket packet) {
        return data.rewriteEntityId(packet.getRuntimeEntityId(), packet::setRuntimeEntityId);
    }

    @Override
    public PacketSignal handle(AddPlayerPacket packet) {
        PacketSignal signal0 = data.rewriteEntityId(packet.getRuntimeEntityId(), packet::setRuntimeEntityId);
        PacketSignal signal1 = data.rewriteEntityId(packet.getUniqueEntityId(), packet::setUniqueEntityId);

        PacketSignal signal2 = PacketSignal.UNHANDLED;

        ListIterator<EntityLinkData> iterator = packet.getEntityLinks().listIterator();
        while (iterator.hasNext()) {
            EntityLinkData entityLink = iterator.next();
            long from = PlayerRewriteUtils.rewriteId(entityLink.getFrom(), this.data.getEntityId(), this.data.getOriginalEntityId());
            long to = PlayerRewriteUtils.rewriteId(entityLink.getTo(), this.data.getEntityId(), this.data.getOriginalEntityId());
            if (entityLink.getFrom() != from || entityLink.getTo() != to) {
                iterator.set(new EntityLinkData(from, to, entityLink.getType(), entityLink.isImmediate(), entityLink.isRiderInitiated()));
                signal2 = PacketSignal.HANDLED;
            }
        }

        PacketSignal signal3 = this.rewriteMetadata(packet.getMetadata());
        return (signal0 == PacketSignal.HANDLED || signal1 == PacketSignal.HANDLED || signal2 == PacketSignal.HANDLED || signal3 == PacketSignal.HANDLED) ?
            PacketSignal.HANDLED : PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(AddEntityPacket packet) {
        PacketSignal signal0 = data.rewriteEntityId(packet.getRuntimeEntityId(), packet::setRuntimeEntityId);
        PacketSignal signal1 = data.rewriteEntityId(packet.getUniqueEntityId(), packet::setUniqueEntityId);

        PacketSignal signal2 = PacketSignal.UNHANDLED;

        ListIterator<EntityLinkData> iterator = packet.getEntityLinks().listIterator();
        while (iterator.hasNext()) {
            EntityLinkData entityLink = iterator.next();
            long from = PlayerRewriteUtils.rewriteId(entityLink.getFrom(), this.data.getEntityId(), this.data.getOriginalEntityId());
            long to = PlayerRewriteUtils.rewriteId(entityLink.getTo(), this.data.getEntityId(), this.data.getOriginalEntityId());
            if (entityLink.getFrom() != from || entityLink.getTo() != to) {
                iterator.set(new EntityLinkData(from, to, entityLink.getType(), entityLink.isImmediate(), entityLink.isRiderInitiated()));
                signal2 = PacketSignal.HANDLED;
            }
        }

        PacketSignal signal4 = this.rewriteMetadata(packet.getMetadata());
        return (signal0 == PacketSignal.HANDLED || signal1 == PacketSignal.HANDLED || signal2 == PacketSignal.HANDLED || signal4 == PacketSignal.HANDLED) ?
            PacketSignal.HANDLED : PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(AddItemEntityPacket packet) {
        PacketSignal signal0 = data.rewriteEntityId(packet.getRuntimeEntityId(), packet::setRuntimeEntityId);
        PacketSignal signal1 = data.rewriteEntityId(packet.getUniqueEntityId(), packet::setUniqueEntityId);
        PacketSignal signal2 = this.rewriteMetadata(packet.getMetadata());
        return (signal0 == PacketSignal.HANDLED || signal1 == PacketSignal.HANDLED || signal2 == PacketSignal.HANDLED) ?
            PacketSignal.HANDLED : PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(AddPaintingPacket packet) {
        PacketSignal signal0 = data.rewriteEntityId(packet.getRuntimeEntityId(), packet::setRuntimeEntityId);
        PacketSignal signal1 = data.rewriteEntityId(packet.getUniqueEntityId(), packet::setUniqueEntityId);
        return mergeSignals(signal0, signal1);
    }

    @Override
    public PacketSignal handle(RemoveEntityPacket packet) {
        return data.rewriteEntityId(packet.getUniqueEntityId(), packet::setUniqueEntityId);
    }

    @Override
    public PacketSignal handle(BossEventPacket packet) {
        PacketSignal signal0 = data.rewriteEntityId(packet.getBossUniqueEntityId(), packet::setBossUniqueEntityId);
        PacketSignal signal1 = data.rewriteEntityId(packet.getPlayerUniqueEntityId(), packet::setPlayerUniqueEntityId);
        return mergeSignals(signal0, signal1);
    }

    @Override
    public PacketSignal handle(TakeItemEntityPacket packet) {
        PacketSignal signal0 = data.rewriteEntityId(packet.getRuntimeEntityId(), packet::setRuntimeEntityId);
        PacketSignal signal1 = data.rewriteEntityId(packet.getItemRuntimeEntityId(), packet::setItemRuntimeEntityId);
        return mergeSignals(signal0, signal1);
    }

    @Override
    public PacketSignal handle(MovePlayerPacket packet) {
        PacketSignal signal0 = data.rewriteEntityId(packet.getRuntimeEntityId(), packet::setRuntimeEntityId);
        PacketSignal signal1 = data.rewriteEntityId(packet.getRidingRuntimeEntityId(), packet::setRidingRuntimeEntityId);
        return mergeSignals(signal0, signal1);
    }

    @Override
    public PacketSignal handle(InteractPacket packet) {
        return data.rewriteEntityId(packet.getRuntimeEntityId(), packet::setRuntimeEntityId);
    }

    @Override
    public PacketSignal handle(PlayerLocationPacket packet) {
        return data.rewriteEntityId(packet.getTargetEntityId(), packet::setTargetEntityId);
    }

    @Override
    public PacketSignal handle(SetEntityLinkPacket packet) {
        EntityLinkData entityLink = packet.getEntityLink();
        long from = PlayerRewriteUtils.rewriteId(entityLink.getFrom(), this.data.getEntityId(), this.data.getOriginalEntityId());
        long to = PlayerRewriteUtils.rewriteId(entityLink.getTo(), this.data.getEntityId(), this.data.getOriginalEntityId());

        if (from != entityLink.getFrom() || to != entityLink.getTo()) {
            packet.setEntityLink(new EntityLinkData(from, to, entityLink.getType(), entityLink.isImmediate(), entityLink.isRiderInitiated()));
            return PacketSignal.HANDLED;
        }
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(AnimatePacket packet) {
        return data.rewriteEntityId(packet.getRuntimeEntityId(), packet::setRuntimeEntityId);
    }

    @Override
    public PacketSignal handle(AdventureSettingsPacket packet) {
        return data.rewriteEntityId(packet.getUniqueEntityId(), packet::setUniqueEntityId);
    }

    @Override
    public PacketSignal handle(PlayerListPacket packet) {
        if (packet.getAction() != PlayerListPacket.Action.ADD) {
            return PacketSignal.UNHANDLED;
        }

        PacketSignal signal = PacketSignal.UNHANDLED;

        for (PlayerListPacket.Entry entry : packet.getEntries()) {
            long rewriteId = PlayerRewriteUtils.rewriteId(entry.getEntityId(), this.data.getEntityId(), this.data.getOriginalEntityId());
            if (rewriteId != entry.getEntityId()) {
                signal = PacketSignal.HANDLED;
                entry.setEntityId(rewriteId);
            }
        }
        return signal;
    }

    @Override
    public PacketSignal handle(UpdateTradePacket packet) {
        PacketSignal signal0 = data.rewriteEntityId(packet.getPlayerUniqueEntityId(), packet::setPlayerUniqueEntityId);
        PacketSignal signal1 = data.rewriteEntityId(packet.getTraderUniqueEntityId(), packet::setTraderUniqueEntityId);
        return mergeSignals(signal0, signal1);
    }

    @Override
    public PacketSignal handle(RespawnPacket packet) {
        return data.rewriteEntityId(packet.getRuntimeEntityId(), packet::setRuntimeEntityId);
    }

    @Override
    public PacketSignal handle(EmoteListPacket packet) {
        return data.rewriteEntityId(packet.getRuntimeEntityId(), packet::setRuntimeEntityId);
    }

    public PacketSignal handle(NpcDialoguePacket packet) {
        return data.rewriteEntityId(packet.getUniqueEntityId(), packet::setUniqueEntityId);
    }

    public PacketSignal handle(NpcRequestPacket packet) {
        return data.rewriteEntityId(packet.getRuntimeEntityId(), packet::setRuntimeEntityId);
    }

    @Override
    public PacketSignal handle(EmotePacket packet) {
        return data.rewriteEntityId(packet.getRuntimeEntityId(), packet::setRuntimeEntityId);
    }

    @Override
    public PacketSignal handle(SpawnParticleEffectPacket packet) {
        return data.rewriteEntityId(packet.getUniqueEntityId(), packet::setUniqueEntityId);
    }

    @Override
    public PacketSignal handle(EntityPickRequestPacket packet) {
        return data.rewriteEntityId(packet.getRuntimeEntityId(), packet::setRuntimeEntityId);
    }

    @Override
    public PacketSignal handle(EventPacket packet) {
        return data.rewriteEntityId(packet.getUniqueEntityId(), packet::setUniqueEntityId);
    }

    @Override
    public PacketSignal handle(UpdatePlayerGameTypePacket packet) {
        return data.rewriteEntityId(packet.getEntityId(), packet::setEntityId);
    }

    @Override
    public PacketSignal handle(UpdateAbilitiesPacket packet) {
        return data.rewriteEntityId(packet.getUniqueEntityId(), packet::setUniqueEntityId);
    }

    @Override
    public PacketSignal handle(ClientCheatAbilityPacket packet) {
        return data.rewriteEntityId(packet.getUniqueEntityId(), packet::setUniqueEntityId);
    }

    @Override
    public PacketSignal handle(PlayerUpdateEntityOverridesPacket packet) {
        return data.rewriteEntityId(packet.getEntityUniqueId(), packet::setEntityUniqueId);
    }

    @Override
    public PacketSignal handle(LevelSoundEventPacket packet) {
        return data.rewriteEntityId(packet.getEntityUniqueId(), packet::setEntityUniqueId);
    }

    @Override
    public PacketSignal handle(AnimateEntityPacket packet) {
        PacketSignal signal = PacketSignal.UNHANDLED;
        LongListIterator iterator = packet.getRuntimeEntityIds().listIterator();
        while (iterator.hasNext()) {
            PacketSignal returnedSignal = data.rewriteEntityId(iterator.nextLong(), iterator::set);
            signal = mergeSignals(signal, returnedSignal);
        }
        return signal;
    }

    @Override
    public PacketSignal handle(MovementEffectPacket packet) {
        return data.rewriteEntityId(packet.getEntityRuntimeId(), packet::setEntityRuntimeId);
    }

    @Override
    public PacketSignal handle(MovementPredictionSyncPacket packet) {
        return data.rewriteEntityId(packet.getRuntimeEntityId(), packet::setRuntimeEntityId);
    }

    @Override
    public PacketSignal handle(UpdateEquipPacket packet) {
        return data.rewriteEntityId(packet.getUniqueEntityId(), packet::setUniqueEntityId);
    }

    @Override
    public PacketSignal handle(CameraInstructionPacket packet) {
        PacketSignal signal = PacketSignal.UNHANDLED;
        CameraAttachToEntityInstruction attachInstruction = packet.getAttachInstruction();
        if (attachInstruction != null) {
            PacketSignal returnedSignal = data.rewriteEntityId(attachInstruction.getUniqueEntityId(), attachInstruction::setUniqueEntityId);
            signal = mergeSignals(signal, returnedSignal);
        }
        return signal;
    }

    @Override
    public PacketSignal handle(DebugDrawerPacket packet) {
        PacketSignal signal = PacketSignal.UNHANDLED;
        for (DebugShape shape : packet.getShapes()) {
            Long attachedEntityId = shape.getAttachedToEntityId();
            if (attachedEntityId != null) {
                PacketSignal returnedSignal = data.rewriteEntityId(attachedEntityId, shape::setAttachedToEntityId);
                signal = mergeSignals(signal, returnedSignal);
            }
        }
        return signal;
    }

    private PacketSignal rewriteMetadata(EntityDataMap metadata) {
        PacketSignal signal = PacketSignal.UNHANDLED;
        for (EntityDataType<Long> data : ENTITY_DATA_FIELDS) {
            Long id = metadata.get(data);
            if (id != null) {
                long rewriteId = PlayerRewriteUtils.rewriteId(id, this.data.getEntityId(), this.data.getOriginalEntityId());
                if (rewriteId != id) {
                    metadata.put(data, rewriteId);
                    signal = PacketSignal.HANDLED;
                }
            }
        }
        return signal;
    }
}
