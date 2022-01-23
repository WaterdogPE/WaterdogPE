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
import com.nukkitx.protocol.bedrock.data.ScoreInfo;
import com.nukkitx.protocol.bedrock.data.entity.EntityLinkData;
import com.nukkitx.protocol.bedrock.handler.BedrockPacketHandler;
import com.nukkitx.protocol.bedrock.packet.*;
import dev.waterdog.waterdogpe.player.PlayerRewriteUtils;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;

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

    public boolean trackEntity(BedrockPacket packet) {
        return packet.handle(this);
    }

    @Override
    public boolean handle(AddPlayerPacket packet) {
        this.player.getEntities().add(packet.getRuntimeEntityId());
        return false;
    }

    @Override
    public boolean handle(AddEntityPacket packet) {
        this.player.getEntities().add(packet.getRuntimeEntityId());
        for (EntityLinkData entityLink : packet.getEntityLinks()) {
            this.handleEntityLink(entityLink);
        }
        return false;
    }

    @Override
    public boolean handle(AddItemEntityPacket packet) {
        this.player.getEntities().add(packet.getRuntimeEntityId());
        return false;
    }

    @Override
    public boolean handle(AddPaintingPacket packet) {
        this.player.getEntities().add(packet.getRuntimeEntityId());
        return false;
    }

    @Override
    public boolean handle(RemoveEntityPacket packet) {
        this.player.getEntities().remove(packet.getUniqueEntityId());
        return false;
    }

    @Override
    public boolean handle(PlayerListPacket packet) {
        List<PlayerListPacket.Entry> entries = packet.getEntries();
        for (PlayerListPacket.Entry entry : entries) {
            if (packet.getAction() == PlayerListPacket.Action.ADD) {
                this.player.getPlayers().add(entry.getUuid());
            } else if (packet.getAction() == PlayerListPacket.Action.REMOVE) {
                this.player.getPlayers().remove(entry.getUuid());
            }
        }
        return false;
    }

    @Override
    public boolean handle(SetEntityLinkPacket packet) {
        this.handleEntityLink(packet.getEntityLink());
        return false;
    }

    private void handleEntityLink(EntityLinkData entityLink) {
        if (entityLink.getType() == EntityLinkData.Type.REMOVE) {
            this.player.getEntityLinks().remove(entityLink.getFrom());
        } else {
            this.player.getEntityLinks().put(entityLink.getFrom(), entityLink.getTo());
        }
    }

    @Override
    public boolean handle(SetEntityDataPacket packet) {
        if (packet.getRuntimeEntityId() == this.player.getRewriteData().getOriginalEntityId()) {
            boolean immobile = PlayerRewriteUtils.checkForImmobileFlag(packet.getMetadata());
            this.player.getRewriteData().setImmobileFlag(immobile);
        }
        return false;
    }

    @Override
    public final boolean handle(SetDisplayObjectivePacket packet) {
        this.player.getScoreboards().add(packet.getObjectiveId());
        return false;
    }

    @Override
    public final boolean handle(RemoveObjectivePacket packet) {
        this.player.getScoreboards().remove(packet.getObjectiveId());
        return false;
    }

    @Override
    public final boolean handle(SetScorePacket packet) {
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
        return false;
    }

    @Override
    public final boolean handle(BossEventPacket packet) {
        switch (packet.getAction()) {
            case CREATE:
                this.player.getBossbars().add(packet.getBossUniqueEntityId());
                break;
            case REMOVE:
                this.player.getBossbars().remove(packet.getBossUniqueEntityId());
                break;
        }
        return false;
    }
}
