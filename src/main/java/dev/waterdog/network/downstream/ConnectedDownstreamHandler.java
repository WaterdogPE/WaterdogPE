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

package dev.waterdog.network.downstream;

import com.nukkitx.protocol.bedrock.packet.*;
import dev.waterdog.command.Command;
import dev.waterdog.event.defaults.PostTransferCompleteEvent;
import dev.waterdog.network.ServerInfo;
import dev.waterdog.network.rewrite.types.RewriteData;
import dev.waterdog.network.session.ServerConnection;
import dev.waterdog.player.ProxiedPlayer;
import dev.waterdog.utils.exceptions.CancelSignalException;
import dev.waterdog.utils.types.TranslationContainer;

public class ConnectedDownstreamHandler extends AbstractDownstreamHandler {

    private final ServerConnection server;

    public ConnectedDownstreamHandler(ProxiedPlayer player, ServerConnection server) {
        super(player);
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
        int sizeBefore = packet.getCommands().size();

        for (Command command : this.player.getProxy().getCommandMap().getCommands().values()) {
            if (command.getPermission() == null || this.player.hasPermission(command.getPermission())) {
                packet.getCommands().add(command.getData());
            }
        }
        return packet.getCommands().size() > sizeBefore;
    }

    @Override
    public boolean handle(PlayStatusPacket packet) {
        if (!this.player.acceptPlayStatus() || packet.getStatus() != PlayStatusPacket.Status.PLAYER_SPAWN) {
            return false;
        }

        this.player.setAcceptPlayStatus(false);
        RewriteData rewriteData = this.player.getRewriteData();

        SetLocalPlayerAsInitializedPacket initializedPacket = new SetLocalPlayerAsInitializedPacket();
        initializedPacket.setRuntimeEntityId(rewriteData.getEntityId());
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

        ServerInfo serverInfo = this.player.getProxy().getServerInfo(packet.getAddress());
        if (serverInfo == null) {
            serverInfo = this.player.getProxy().getServerInfo(packet.getAddress(), packet.getPort());
        }

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
