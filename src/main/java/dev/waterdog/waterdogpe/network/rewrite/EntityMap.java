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

package dev.waterdog.waterdogpe.network.rewrite;

import com.nukkitx.protocol.bedrock.BedrockPacket;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.data.entity.EntityLinkData;
import com.nukkitx.protocol.bedrock.handler.BedrockPacketHandler;
import com.nukkitx.protocol.bedrock.packet.*;
import dev.waterdog.waterdogpe.network.rewrite.types.RewriteData;
import dev.waterdog.waterdogpe.player.PlayerRewriteUtils;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;

import java.util.ListIterator;

/**
 * Class to map the proper entityIds to entity-related packets.
 */
public class EntityMap implements BedrockPacketHandler {

    private final ProxiedPlayer player;
    private final RewriteData rewrite;

    public EntityMap(ProxiedPlayer player) {
        this.player = player;
        this.rewrite = player.getRewriteData();
    }

    public boolean doRewrite(BedrockPacket packet) {
        return this.player.canRewrite() && packet.handle(this);
    }

    @Override
    public boolean handle(MoveEntityAbsolutePacket packet) {
        packet.setRuntimeEntityId(PlayerRewriteUtils.rewriteId(packet.getRuntimeEntityId(), this.rewrite.getEntityId(), this.rewrite.getOriginalEntityId()));
        return true;
    }

    @Override
    public boolean handle(EntityEventPacket packet) {
        packet.setRuntimeEntityId(PlayerRewriteUtils.rewriteId(packet.getRuntimeEntityId(), this.rewrite.getEntityId(), this.rewrite.getOriginalEntityId()));
        return true;
    }

    @Override
    public boolean handle(MobEffectPacket packet) {
        packet.setRuntimeEntityId(PlayerRewriteUtils.rewriteId(packet.getRuntimeEntityId(), this.rewrite.getEntityId(), this.rewrite.getOriginalEntityId()));
        return true;
    }

    @Override
    public boolean handle(UpdateAttributesPacket packet) {
        packet.setRuntimeEntityId(PlayerRewriteUtils.rewriteId(packet.getRuntimeEntityId(), this.rewrite.getEntityId(), this.rewrite.getOriginalEntityId()));
        return true;
    }

    @Override
    public boolean handle(MobEquipmentPacket packet) {
        packet.setRuntimeEntityId(PlayerRewriteUtils.rewriteId(packet.getRuntimeEntityId(), this.rewrite.getEntityId(), this.rewrite.getOriginalEntityId()));
        return true;
    }

    @Override
    public boolean handle(MobArmorEquipmentPacket packet) {
        packet.setRuntimeEntityId(PlayerRewriteUtils.rewriteId(packet.getRuntimeEntityId(), this.rewrite.getEntityId(), this.rewrite.getOriginalEntityId()));
        return true;
    }

    @Override
    public boolean handle(PlayerActionPacket packet) {
        packet.setRuntimeEntityId(PlayerRewriteUtils.rewriteId(packet.getRuntimeEntityId(), this.rewrite.getEntityId(), this.rewrite.getOriginalEntityId()));
        return true;
    }

    @Override
    public boolean handle(SetEntityDataPacket packet) {
        packet.setRuntimeEntityId(PlayerRewriteUtils.rewriteId(packet.getRuntimeEntityId(), rewrite.getEntityId(), this.rewrite.getOriginalEntityId()));
        PlayerRewriteUtils.rewriteEntityMetadata(packet.getMetadata(), this.rewrite.getEntityId(), this.rewrite.getOriginalEntityId());
        return true;
    }

    @Override
    public boolean handle(SetEntityMotionPacket packet) {
        packet.setRuntimeEntityId(PlayerRewriteUtils.rewriteId(packet.getRuntimeEntityId(), this.rewrite.getEntityId(), this.rewrite.getOriginalEntityId()));
        return true;
    }

    @Override
    public boolean handle(MoveEntityDeltaPacket packet) {
        packet.setRuntimeEntityId(PlayerRewriteUtils.rewriteId(packet.getRuntimeEntityId(), this.rewrite.getEntityId(), this.rewrite.getOriginalEntityId()));
        return true;
    }

    @Override
    public boolean handle(SetLocalPlayerAsInitializedPacket packet) {
        packet.setRuntimeEntityId(PlayerRewriteUtils.rewriteId(packet.getRuntimeEntityId(), this.rewrite.getEntityId(), this.rewrite.getOriginalEntityId()));
        return true;
    }

    @Override
    public boolean handle(AddPlayerPacket packet) {
        packet.setRuntimeEntityId(PlayerRewriteUtils.rewriteId(packet.getRuntimeEntityId(), this.rewrite.getEntityId(), this.rewrite.getOriginalEntityId()));
        packet.setUniqueEntityId(PlayerRewriteUtils.rewriteId(packet.getUniqueEntityId(), this.rewrite.getEntityId(), this.rewrite.getOriginalEntityId()));
        // Currently unused, but don't forget about this
        // PlayerRewriteUtils.rewriteEntityMetadata(packet.getMetadata(), this.rewrite.getEntityId(), this.rewrite.getOriginalEntityId());

        ListIterator<EntityLinkData> iterator = packet.getEntityLinks().listIterator();
        while (iterator.hasNext()) {
            EntityLinkData entityLink = iterator.next();
            long from = PlayerRewriteUtils.rewriteId(entityLink.getFrom(), this.rewrite.getEntityId(), this.rewrite.getOriginalEntityId());
            long to = PlayerRewriteUtils.rewriteId(entityLink.getTo(), this.rewrite.getEntityId(), this.rewrite.getOriginalEntityId());
            if (entityLink.getFrom() != from || entityLink.getTo() != to) {
                iterator.set(new EntityLinkData(from, to, entityLink.getType(), entityLink.isImmediate(), entityLink.isRiderInitiated()));
            }
        }
        return true;
    }

    @Override
    public boolean handle(AddEntityPacket packet) {
        packet.setRuntimeEntityId(PlayerRewriteUtils.rewriteId(packet.getRuntimeEntityId(), this.rewrite.getEntityId(), this.rewrite.getOriginalEntityId()));
        packet.setUniqueEntityId(PlayerRewriteUtils.rewriteId(packet.getUniqueEntityId(), this.rewrite.getEntityId(), this.rewrite.getOriginalEntityId()));
        // Currently unused, but don't forget about this
        // PlayerRewriteUtils.rewriteEntityMetadata(packet.getMetadata(), this.rewrite.getEntityId(), this.rewrite.getOriginalEntityId());

        ListIterator<EntityLinkData> iterator = packet.getEntityLinks().listIterator();
        while (iterator.hasNext()) {
            EntityLinkData entityLink = iterator.next();
            long from = PlayerRewriteUtils.rewriteId(entityLink.getFrom(), this.rewrite.getEntityId(), this.rewrite.getOriginalEntityId());
            long to = PlayerRewriteUtils.rewriteId(entityLink.getTo(), this.rewrite.getEntityId(), this.rewrite.getOriginalEntityId());
            if (entityLink.getFrom() != from || entityLink.getTo() != to) {
                iterator.set(new EntityLinkData(from, to, entityLink.getType(), entityLink.isImmediate(), entityLink.isRiderInitiated()));
            }
        }
        return true;
    }

    @Override
    public boolean handle(AddItemEntityPacket packet) {
        packet.setRuntimeEntityId(PlayerRewriteUtils.rewriteId(packet.getRuntimeEntityId(), this.rewrite.getEntityId(), this.rewrite.getOriginalEntityId()));
        packet.setUniqueEntityId(PlayerRewriteUtils.rewriteId(packet.getUniqueEntityId(), this.rewrite.getEntityId(), this.rewrite.getOriginalEntityId()));
        // Currently unused, but don't forget about this
        // PlayerRewriteUtils.rewriteEntityMetadata(packet.getMetadata(), this.rewrite.getEntityId(), this.rewrite.getOriginalEntityId());
        return true;
    }

    @Override
    public boolean handle(AddPaintingPacket packet) {
        packet.setRuntimeEntityId(PlayerRewriteUtils.rewriteId(packet.getRuntimeEntityId(), this.rewrite.getEntityId(), this.rewrite.getOriginalEntityId()));
        packet.setUniqueEntityId(PlayerRewriteUtils.rewriteId(packet.getUniqueEntityId(), this.rewrite.getEntityId(), this.rewrite.getOriginalEntityId()));
        return true;
    }

    @Override
    public boolean handle(RemoveEntityPacket packet) {
        packet.setUniqueEntityId(PlayerRewriteUtils.rewriteId(packet.getUniqueEntityId(), this.rewrite.getEntityId(), this.rewrite.getOriginalEntityId()));
        return true;
    }

    @Override
    public boolean handle(BossEventPacket packet) {
        packet.setBossUniqueEntityId(PlayerRewriteUtils.rewriteId(packet.getBossUniqueEntityId(), this.rewrite.getEntityId(), this.rewrite.getOriginalEntityId()));
        packet.setPlayerUniqueEntityId(PlayerRewriteUtils.rewriteId(packet.getPlayerUniqueEntityId(), this.rewrite.getEntityId(), this.rewrite.getOriginalEntityId()));
        return true;
    }

    @Override
    public boolean handle(TakeItemEntityPacket packet) {
        packet.setRuntimeEntityId(PlayerRewriteUtils.rewriteId(packet.getRuntimeEntityId(), this.rewrite.getEntityId(), this.rewrite.getOriginalEntityId()));
        packet.setItemRuntimeEntityId(PlayerRewriteUtils.rewriteId(packet.getItemRuntimeEntityId(), this.rewrite.getEntityId(), this.rewrite.getOriginalEntityId()));
        return true;
    }

    @Override
    public boolean handle(MovePlayerPacket packet) {
        packet.setRuntimeEntityId(PlayerRewriteUtils.rewriteId(packet.getRuntimeEntityId(), this.rewrite.getEntityId(), this.rewrite.getOriginalEntityId()));
        packet.setRidingRuntimeEntityId(PlayerRewriteUtils.rewriteId(packet.getRidingRuntimeEntityId(), this.rewrite.getEntityId(), this.rewrite.getOriginalEntityId()));
        return true;
    }

    @Override
    public boolean handle(InteractPacket packet) {
        packet.setRuntimeEntityId(PlayerRewriteUtils.rewriteId(packet.getRuntimeEntityId(), this.rewrite.getEntityId(), this.rewrite.getOriginalEntityId()));
        return true;
    }

    @Override
    public boolean handle(SetEntityLinkPacket packet) {
        EntityLinkData entityLink = packet.getEntityLink();
        long from = PlayerRewriteUtils.rewriteId(entityLink.getFrom(), this.rewrite.getEntityId(), this.rewrite.getOriginalEntityId());
        long to = PlayerRewriteUtils.rewriteId(entityLink.getTo(), this.rewrite.getEntityId(), this.rewrite.getOriginalEntityId());

        packet.setEntityLink(new EntityLinkData(from, to, entityLink.getType(), entityLink.isImmediate(), entityLink.isRiderInitiated()));
        return true;
    }

    @Override
    public boolean handle(AnimatePacket packet) {
        packet.setRuntimeEntityId(PlayerRewriteUtils.rewriteId(packet.getRuntimeEntityId(), this.rewrite.getEntityId(), this.rewrite.getOriginalEntityId()));
        return true;
    }

    @Override
    public boolean handle(AdventureSettingsPacket packet) {
        packet.setUniqueEntityId(PlayerRewriteUtils.rewriteId(packet.getUniqueEntityId(), this.rewrite.getEntityId(), this.rewrite.getOriginalEntityId()));
        return true;
    }

    @Override
    public boolean handle(PlayerListPacket packet) {
        if (packet.getAction() != PlayerListPacket.Action.ADD) {
            return false;
        }

        for (PlayerListPacket.Entry entry : packet.getEntries()) {
            entry.setEntityId(PlayerRewriteUtils.rewriteId(entry.getEntityId(), this.rewrite.getEntityId(), this.rewrite.getOriginalEntityId()));
        }
        return true;
    }

    @Override
    public boolean handle(UpdateTradePacket packet) {
        packet.setPlayerUniqueEntityId(PlayerRewriteUtils.rewriteId(packet.getPlayerUniqueEntityId(), this.rewrite.getEntityId(), this.rewrite.getOriginalEntityId()));
        packet.setTraderUniqueEntityId(PlayerRewriteUtils.rewriteId(packet.getTraderUniqueEntityId(), this.rewrite.getEntityId(), this.rewrite.getOriginalEntityId()));
        return true;
    }

    @Override
    public boolean handle(RespawnPacket packet) {
        packet.setRuntimeEntityId(PlayerRewriteUtils.rewriteId(packet.getRuntimeEntityId(), this.rewrite.getEntityId(), this.rewrite.getOriginalEntityId()));
        return true;
    }

    @Override
    public boolean handle(EmoteListPacket packet) {
        packet.setRuntimeEntityId(PlayerRewriteUtils.rewriteId(packet.getRuntimeEntityId(), this.rewrite.getEntityId(), this.rewrite.getOriginalEntityId()));
        return true;
    }

    public boolean handle(NpcDialoguePacket packet) {
        packet.setUniqueEntityId(PlayerRewriteUtils.rewriteId(packet.getUniqueEntityId(), this.rewrite.getEntityId(), this.rewrite.getOriginalEntityId()));
        return true;
    }

    public boolean handle(NpcRequestPacket packet) {
        packet.setRuntimeEntityId(PlayerRewriteUtils.rewriteId(packet.getRuntimeEntityId(), this.rewrite.getEntityId(), this.rewrite.getOriginalEntityId()));
        return true;
    }

    @Override
    public boolean handle(EmotePacket packet) {
        packet.setRuntimeEntityId(PlayerRewriteUtils.rewriteId(packet.getRuntimeEntityId(), this.rewrite.getEntityId(), this.rewrite.getOriginalEntityId()));
        return true;
    }

    @Override
    public boolean handle(EntityPickRequestPacket packet) {
        packet.setRuntimeEntityId(PlayerRewriteUtils.rewriteId(packet.getRuntimeEntityId(), this.rewrite.getEntityId(), this.rewrite.getOriginalEntityId()));
        return true;
    }

    @Override
    public boolean handle(UpdateAbilitiesPacket packet) {
        packet.setUniqueEntityId(PlayerRewriteUtils.rewriteId(packet.getUniqueEntityId(), this.rewrite.getEntityId(), this.rewrite.getOriginalEntityId()));
        return true;
    }
}
