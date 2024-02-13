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

package dev.waterdog.waterdogpe.network.protocol.handler.upstream;

import dev.waterdog.waterdogpe.network.connection.ProxiedConnection;
import dev.waterdog.waterdogpe.network.connection.client.ClientConnection;
import dev.waterdog.waterdogpe.network.protocol.handler.ProxyPacketHandler;
import dev.waterdog.waterdogpe.network.protocol.rewrite.RewriteMaps;
import lombok.Setter;
import org.cloudburstmc.protocol.bedrock.data.PlayerActionType;
import org.cloudburstmc.protocol.bedrock.netty.BedrockBatchWrapper;
import org.cloudburstmc.protocol.bedrock.packet.*;
import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.event.defaults.PlayerChatEvent;
import dev.waterdog.waterdogpe.network.protocol.ProtocolVersion;
import dev.waterdog.waterdogpe.network.protocol.handler.TransferCallback;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import dev.waterdog.waterdogpe.network.protocol.Signals;
import org.cloudburstmc.protocol.common.PacketSignal;

/**
 * Main handler for handling packets received from upstream.
 */
public class ConnectedUpstreamHandler extends AbstractUpstreamHandler implements ProxyPacketHandler {

    @Setter
    private ClientConnection targetConnection;

    public ConnectedUpstreamHandler(ProxiedPlayer player) {
        super(player);
    }

    @Override
    public void sendProxiedBatch(BedrockBatchWrapper batch) {
        if (this.targetConnection != null && this.targetConnection.isConnected()) {
            this.targetConnection.sendPacket(batch.retain());
        }
    }

    @Override
    public final PacketSignal handle(RequestChunkRadiusPacket packet) {
        this.player.getLoginData().setChunkRadius(packet);
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(PlayerActionPacket packet) {
        if (packet.getAction() != PlayerActionType.DIMENSION_CHANGE_SUCCESS) {
            return PacketSignal.UNHANDLED;
        }

        TransferCallback transferCallback = this.player.getRewriteData().getTransferCallback();
        if (transferCallback != null && transferCallback.onDimChangeSuccess()) {
            return Signals.CANCEL;
        }
        return PacketSignal.UNHANDLED;
    }

    @Override
    public final PacketSignal handle(TextPacket packet) {
        PlayerChatEvent event = new PlayerChatEvent(this.player, packet.getMessage());
        ProxyServer.getInstance().getEventManager().callEvent(event);
        packet.setMessage(event.getMessage());
        if (event.isCancelled()) {
            return Signals.CANCEL;
        }
        return PacketSignal.HANDLED;
    }

    @Override
    public final PacketSignal handle(CommandRequestPacket packet) {
        String message = packet.getCommand();
        if (this.player.getProxy().handlePlayerCommand(this.player, message)) {
            return Signals.CANCEL;
        }
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(ClientCacheBlobStatusPacket packet) {
        if (this.player.getProtocol().isBefore(ProtocolVersion.MINECRAFT_PE_1_18_30)) {
            this.player.getChunkBlobs().addAll(packet.getNaks());
        }
        return PacketSignal.UNHANDLED;
    }

    @Override
    public boolean isForceEncode() {
        return false;
    }

    @Override
    public ProxiedConnection getConnection() {
        return this.targetConnection;
    }

    @Override
    public RewriteMaps getRewriteMaps() {
        return this.player.getRewriteMaps();
    }
}
