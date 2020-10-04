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

package pe.waterdog.network.downstream;

import com.nukkitx.protocol.bedrock.handler.BedrockPacketHandler;
import com.nukkitx.protocol.bedrock.packet.DisconnectPacket;
import com.nukkitx.protocol.bedrock.packet.BossEventPacket;
import com.nukkitx.protocol.bedrock.packet.RemoveObjectivePacket;
import com.nukkitx.protocol.bedrock.packet.SetDisplayObjectivePacket;
import pe.waterdog.network.ServerInfo;
import pe.waterdog.network.session.ServerConnection;
import pe.waterdog.player.ProxiedPlayer;

public class ConnectedDownstreamHandler implements BedrockPacketHandler {

    private final ProxiedPlayer player;
    private final ServerConnection server;

    public ConnectedDownstreamHandler(ProxiedPlayer player, ServerConnection server) {
        this.player = player;
        this.server = server;
    }

    @Override
    public boolean handle(SetDisplayObjectivePacket packet) {
        this.player.getScoreboards().add(packet.getObjectiveId());
        return false;
    }

    @Override
    public boolean handle(RemoveObjectivePacket packet) {
        this.player.getScoreboards().remove(packet.getObjectiveId());
        return false;
    }

    @Override
    public boolean handle(BossEventPacket packet) {
        switch (packet.getAction()){
            case CREATE:
                this.player.getBossbars().add(packet.getBossUniqueEntityId());
            case REMOVE:
                this.player.getBossbars().remove(packet.getBossUniqueEntityId());
        }
        return false;
    }

    @Override
    public boolean handle(DisconnectPacket packet) {
        ServerInfo i = this.player.getProxy().getReconnectHandler().getFallbackServer(this.player, this.server.getInfo());
        if (i != null) {
            this.player.connect(i);
        } else {
            //TODO Fix Translation Container Freezing Thread
            this.player.disconnect("§cYou were kicked. Reason: " + packet.getKickMessage());
        }
        return true;
    }
}
