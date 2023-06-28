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
import org.cloudburstmc.protocol.bedrock.data.ScoreInfo;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityLinkData;
import org.cloudburstmc.protocol.bedrock.packet.*;
import dev.waterdog.waterdogpe.network.protocol.user.PlayerRewriteUtils;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import org.cloudburstmc.protocol.common.PacketSignal;

import java.util.List;

/**
 * Pipeline Handler used to track entities of any kind, aswell as other data
 * that will be required to get removed when switching servers.
 */
public class EntityTracker implements BedrockPacketHandler {

    private final ProxiedPlayer player;

    public EntityTracker(ProxiedPlayer player) {
        this.player = player;
    }

    public PacketSignal trackEntity(BedrockPacket packet) {
        return this.handlePacket(packet);
    }

    @Override
    public PacketSignal handle(AddPlayerPacket packet) {
        this.player.getEntities().add(packet.getRuntimeEntityId());
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(AddEntityPacket packet) {
        this.player.getEntities().add(packet.getRuntimeEntityId());
        for (EntityLinkData entityLink : packet.getEntityLinks()) {
            this.handleEntityLink(entityLink);
        }
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(AddItemEntityPacket packet) {
        this.player.getEntities().add(packet.getRuntimeEntityId());
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(AddPaintingPacket packet) {
        this.player.getEntities().add(packet.getRuntimeEntityId());
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(RemoveEntityPacket packet) {
        this.player.getEntities().remove(packet.getUniqueEntityId());
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(PlayerListPacket packet) {
        List<PlayerListPacket.Entry> entries = packet.getEntries();
        for (PlayerListPacket.Entry entry : entries) {
            if (packet.getAction() == PlayerListPacket.Action.ADD) {
                this.player.getPlayers().add(entry.getUuid());
            } else if (packet.getAction() == PlayerListPacket.Action.REMOVE) {
                this.player.getPlayers().remove(entry.getUuid());
            }
        }
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(SetEntityLinkPacket packet) {
        this.handleEntityLink(packet.getEntityLink());
        return PacketSignal.UNHANDLED;
    }

    private void handleEntityLink(EntityLinkData entityLink) {
        if (entityLink.getType() == EntityLinkData.Type.REMOVE) {
            this.player.getEntityLinks().remove(entityLink.getFrom());
        } else {
            this.player.getEntityLinks().put(entityLink.getFrom(), entityLink.getTo());
        }
    }

    @Override
    public PacketSignal handle(SetEntityDataPacket packet) {
        if (packet.getRuntimeEntityId() == this.player.getRewriteData().getOriginalEntityId()) {
            boolean immobile = PlayerRewriteUtils.checkForImmobileFlag(packet.getMetadata());
            this.player.getRewriteData().setImmobileFlag(immobile);
        }
        return PacketSignal.UNHANDLED;
    }

    @Override
    public final PacketSignal handle(SetDisplayObjectivePacket packet) {
        this.player.getScoreboards().add(packet.getObjectiveId());
        return PacketSignal.UNHANDLED;
    }

    @Override
    public final PacketSignal handle(RemoveObjectivePacket packet) {
        this.player.getScoreboards().remove(packet.getObjectiveId());
        return PacketSignal.UNHANDLED;
    }

    @Override
    public final PacketSignal handle(SetScorePacket packet) {
        switch(packet.getAction()) {
            case SET:
                for(ScoreInfo info : packet.getInfos()) {
                    this.player.getScoreInfos().put(info.getScoreboardId(), info);
                }
                break;
            case REMOVE:
                for(ScoreInfo info : packet.getInfos()) {
                    this.player.getScoreInfos().remove(info.getScoreboardId());
                }
                break;
        }
        return PacketSignal.UNHANDLED;
    }

    @Override
    public final PacketSignal handle(BossEventPacket packet) {
        switch (packet.getAction()) {
            case CREATE -> this.player.getBossbars().add(packet.getBossUniqueEntityId());
            case REMOVE -> this.player.getBossbars().remove(packet.getBossUniqueEntityId());
        }
        return PacketSignal.UNHANDLED;
    }
}
