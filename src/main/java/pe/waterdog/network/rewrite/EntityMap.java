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

package pe.waterdog.network.rewrite;

import com.nukkitx.protocol.bedrock.BedrockPacket;
import com.nukkitx.protocol.bedrock.data.entity.EntityLinkData;
import com.nukkitx.protocol.bedrock.handler.BedrockPacketHandler;
import com.nukkitx.protocol.bedrock.packet.*;
import pe.waterdog.network.rewrite.types.RewriteData;
import pe.waterdog.player.PlayerRewriteUtils;
import pe.waterdog.player.ProxiedPlayer;

import java.util.ArrayList;
import java.util.List;

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
        packet.setRuntimeEntityId(PlayerRewriteUtils.rewriteId(packet.getRuntimeEntityId(), rewrite.getEntityId(), rewrite.getOriginalEntityId()));
        return true;
    }

    @Override
    public boolean handle(EntityEventPacket packet) {
        packet.setRuntimeEntityId(PlayerRewriteUtils.rewriteId(packet.getRuntimeEntityId(), rewrite.getEntityId(), rewrite.getOriginalEntityId()));
        return true;
    }

    @Override
    public boolean handle(MobEffectPacket packet) {
        packet.setRuntimeEntityId(PlayerRewriteUtils.rewriteId(packet.getRuntimeEntityId(), rewrite.getEntityId(), rewrite.getOriginalEntityId()));
        return true;
    }

    @Override
    public boolean handle(UpdateAttributesPacket packet) {
        packet.setRuntimeEntityId(PlayerRewriteUtils.rewriteId(packet.getRuntimeEntityId(), rewrite.getEntityId(), rewrite.getOriginalEntityId()));
        return true;
    }

    @Override
    public boolean handle(MobEquipmentPacket packet) {
        packet.setRuntimeEntityId(PlayerRewriteUtils.rewriteId(packet.getRuntimeEntityId(), rewrite.getEntityId(), rewrite.getOriginalEntityId()));
        return true;
    }

    @Override
    public boolean handle(PlayerActionPacket packet) {
        packet.setRuntimeEntityId(PlayerRewriteUtils.rewriteId(packet.getRuntimeEntityId(), rewrite.getEntityId(), rewrite.getOriginalEntityId()));
        return true;
    }

    @Override
    public boolean handle(SetEntityDataPacket packet) {
        packet.setRuntimeEntityId(PlayerRewriteUtils.rewriteId(packet.getRuntimeEntityId(), rewrite.getEntityId(), rewrite.getOriginalEntityId()));
        return true;
    }

    @Override
    public boolean handle(SetEntityMotionPacket packet) {
        packet.setRuntimeEntityId(PlayerRewriteUtils.rewriteId(packet.getRuntimeEntityId(), rewrite.getEntityId(), rewrite.getOriginalEntityId()));
        return true;
    }

    @Override
    public boolean handle(MoveEntityDeltaPacket packet) {
        packet.setRuntimeEntityId(PlayerRewriteUtils.rewriteId(packet.getRuntimeEntityId(), rewrite.getEntityId(), rewrite.getOriginalEntityId()));
        return true;
    }

    @Override
    public boolean handle(SetLocalPlayerAsInitializedPacket packet) {
        packet.setRuntimeEntityId(PlayerRewriteUtils.rewriteId(packet.getRuntimeEntityId(), rewrite.getEntityId(), rewrite.getOriginalEntityId()));
        return true;
    }

    @Override
    public boolean handle(AddPlayerPacket packet) {
        packet.setRuntimeEntityId(PlayerRewriteUtils.rewriteId(packet.getRuntimeEntityId(), rewrite.getEntityId(), rewrite.getOriginalEntityId()));
        packet.setUniqueEntityId(PlayerRewriteUtils.rewriteId(packet.getUniqueEntityId(), rewrite.getEntityId(), rewrite.getOriginalEntityId()));
        return true;
    }

    @Override
    public boolean handle(AddEntityPacket packet) {
        packet.setRuntimeEntityId(PlayerRewriteUtils.rewriteId(packet.getRuntimeEntityId(), rewrite.getEntityId(), rewrite.getOriginalEntityId()));
        packet.setUniqueEntityId(PlayerRewriteUtils.rewriteId(packet.getUniqueEntityId(), rewrite.getEntityId(), rewrite.getOriginalEntityId()));
        return true;
    }

    @Override
    public boolean handle(AddItemEntityPacket packet) {
        packet.setRuntimeEntityId(PlayerRewriteUtils.rewriteId(packet.getRuntimeEntityId(), rewrite.getEntityId(), rewrite.getOriginalEntityId()));
        packet.setUniqueEntityId(PlayerRewriteUtils.rewriteId(packet.getUniqueEntityId(), rewrite.getEntityId(), rewrite.getOriginalEntityId()));
        return true;
    }

    @Override
    public boolean handle(AddPaintingPacket packet) {
        packet.setRuntimeEntityId(PlayerRewriteUtils.rewriteId(packet.getRuntimeEntityId(), rewrite.getEntityId(), rewrite.getOriginalEntityId()));
        packet.setUniqueEntityId(PlayerRewriteUtils.rewriteId(packet.getUniqueEntityId(), rewrite.getEntityId(), rewrite.getOriginalEntityId()));
        return true;
    }

    @Override
    public boolean handle(RemoveEntityPacket packet) {
        packet.setUniqueEntityId(PlayerRewriteUtils.rewriteId(packet.getUniqueEntityId(), rewrite.getEntityId(), rewrite.getOriginalEntityId()));
        return true;
    }

    @Override
    public boolean handle(BossEventPacket packet) {
        packet.setBossUniqueEntityId(PlayerRewriteUtils.rewriteId(packet.getBossUniqueEntityId(), rewrite.getEntityId(), rewrite.getOriginalEntityId()));
        packet.setPlayerUniqueEntityId(PlayerRewriteUtils.rewriteId(packet.getPlayerUniqueEntityId(), rewrite.getEntityId(), rewrite.getOriginalEntityId()));
        return true;
    }

    @Override
    public boolean handle(TakeItemEntityPacket packet) {
        packet.setRuntimeEntityId(PlayerRewriteUtils.rewriteId(packet.getRuntimeEntityId(), rewrite.getEntityId(), rewrite.getOriginalEntityId()));
        packet.setItemRuntimeEntityId(PlayerRewriteUtils.rewriteId(packet.getItemRuntimeEntityId(), rewrite.getEntityId(), rewrite.getOriginalEntityId()));
        return true;
    }

    @Override
    public boolean handle(MovePlayerPacket packet) {
        packet.setRuntimeEntityId(PlayerRewriteUtils.rewriteId(packet.getRuntimeEntityId(), rewrite.getEntityId(), rewrite.getOriginalEntityId()));
        packet.setRidingRuntimeEntityId(PlayerRewriteUtils.rewriteId(packet.getRidingRuntimeEntityId(), rewrite.getEntityId(), rewrite.getOriginalEntityId()));
        return true;
    }

    @Override
    public boolean handle(InteractPacket packet) {
        packet.setRuntimeEntityId(PlayerRewriteUtils.rewriteId(packet.getRuntimeEntityId(), rewrite.getEntityId(), rewrite.getOriginalEntityId()));
        return true;
    }

    @Override
    public boolean handle(SetEntityLinkPacket packet) {
        EntityLinkData entityLink = packet.getEntityLink();
        long from = PlayerRewriteUtils.rewriteId(entityLink.getFrom(), rewrite.getEntityId(), rewrite.getOriginalEntityId());
        long to = PlayerRewriteUtils.rewriteId(entityLink.getTo(), rewrite.getEntityId(), rewrite.getOriginalEntityId());

        packet.setEntityLink(new EntityLinkData(from, to, entityLink.getType(), entityLink.isImmediate(), entityLink.isRiderInitiated()));
        return true;
    }

    @Override
    public boolean handle(AnimatePacket packet) {
        packet.setRuntimeEntityId(PlayerRewriteUtils.rewriteId(packet.getRuntimeEntityId(), rewrite.getEntityId(), rewrite.getOriginalEntityId()));
        return true;
    }

    @Override
    public boolean handle(AdventureSettingsPacket packet) {
        packet.setUniqueEntityId(PlayerRewriteUtils.rewriteId(packet.getUniqueEntityId(), rewrite.getEntityId(), rewrite.getOriginalEntityId()));
        return true;
    }

    @Override
    public boolean handle(PlayerListPacket packet) {
        if (packet.getAction() != PlayerListPacket.Action.ADD) return false;

        List<PlayerListPacket.Entry> entries = new ArrayList<>(packet.getEntries());
        packet.getEntries().clear();

        for (PlayerListPacket.Entry entry : entries) {
            entry.setEntityId(PlayerRewriteUtils.rewriteId(entry.getEntityId(), rewrite.getEntityId(), rewrite.getOriginalEntityId()));
            packet.getEntries().add(entry);
        }

        return true;
    }

    @Override
    public boolean handle(UpdateTradePacket packet) {
        packet.setPlayerUniqueEntityId(PlayerRewriteUtils.rewriteId(packet.getPlayerUniqueEntityId(), rewrite.getEntityId(), rewrite.getOriginalEntityId()));
        packet.setTraderUniqueEntityId(PlayerRewriteUtils.rewriteId(packet.getTraderUniqueEntityId(), rewrite.getEntityId(), rewrite.getOriginalEntityId()));
        return true;
    }

    @Override
    public boolean handle(RespawnPacket packet) {
        packet.setRuntimeEntityId(PlayerRewriteUtils.rewriteId(packet.getRuntimeEntityId(), rewrite.getEntityId(), rewrite.getOriginalEntityId()));
        return true;
    }

}
