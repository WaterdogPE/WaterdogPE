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
import com.nukkitx.protocol.bedrock.packet.*;
import pe.waterdog.command.Command;
import pe.waterdog.event.defaults.PostTransferCompleteEvent;
import pe.waterdog.network.ServerInfo;
import pe.waterdog.network.rewrite.types.RewriteData;
import pe.waterdog.network.session.ServerConnection;
import pe.waterdog.player.ProxiedPlayer;
import pe.waterdog.utils.exceptions.CancelSignalException;
import pe.waterdog.utils.types.TranslationContainer;

public class ConnectedDownstreamHandler implements BedrockPacketHandler {

    private final ProxiedPlayer player;
    private final ServerConnection server;

    public ConnectedDownstreamHandler(ProxiedPlayer player, ServerConnection server) {
        this.player = player;
        this.server = server;
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
    public final boolean handle(BossEventPacket packet) {
        switch (packet.getAction()) {
            case CREATE:
                this.player.getBossbars().add(packet.getBossUniqueEntityId());
            case REMOVE:
                this.player.getBossbars().remove(packet.getBossUniqueEntityId());
        }
        return false;
    }

    @Override
    public boolean handle(AvailableCommandsPacket packet) {
        if (!this.player.getProxy().getConfiguration().injectCommands()) {
            return false;
        }

        for (Command cmd : this.player.getProxy().getCommandMap().getCommands().values()) {
            if (cmd.getPermission() != null || (this.player.hasPermission(cmd.getPermission()))) {
                packet.getCommands().add(cmd.getData());
            }
        }
        return true;
    }

    @Override
    public boolean handle(PlayStatusPacket packet) {
        if (!this.player.acceptPlayStatus() || packet.getStatus() != PlayStatusPacket.Status.PLAYER_SPAWN) {
            return false;
        }

        this.player.setAcceptPlayStatus(false);
        RewriteData rewriteData = player.getRewriteData();

        SetLocalPlayerAsInitializedPacket initializedPacket = new SetLocalPlayerAsInitializedPacket();
        initializedPacket.setRuntimeEntityId(rewriteData.getOriginalEntityId());
        this.server.sendPacket(initializedPacket);

        PostTransferCompleteEvent event = new PostTransferCompleteEvent(this.server, this.player);
        this.player.getProxy().getEventManager().callEvent(event);
        return false;
    }

    @Override
    public boolean handle(TransferPacket packet) {
        if (!this.player.getProxy().getConfiguration().useFastTransfer()) {
            return false;
        }

        ServerInfo serverInfo = this.player.getProxy().getServerInfo(packet.getAddress(), packet.getPort());
        if (serverInfo != null) {
            this.player.connect(serverInfo);
            throw CancelSignalException.CANCEL;
        }
        return false;
    }

    @Override
    public final boolean handle(DisconnectPacket packet) {
        if (this.player.sendToFallback(this.server.getInfo(), packet.getKickMessage())) {
            throw CancelSignalException.CANCEL;
        }
        this.player.disconnect(new TranslationContainer("waterdog.downstream.kicked", packet.getKickMessage()));
        throw CancelSignalException.CANCEL;
    }
}
