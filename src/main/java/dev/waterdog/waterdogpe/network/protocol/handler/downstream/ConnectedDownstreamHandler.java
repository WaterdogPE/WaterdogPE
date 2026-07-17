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

package dev.waterdog.waterdogpe.network.protocol.handler.downstream;

import dev.waterdog.waterdogpe.network.connection.client.ClientConnection;
import dev.waterdog.waterdogpe.network.connection.handler.ReconnectReason;
import dev.waterdog.waterdogpe.network.protocol.handler.PluginPacketHandler;
import org.cloudburstmc.protocol.bedrock.PacketDirection;
import org.cloudburstmc.protocol.bedrock.packet.*;
import dev.waterdog.waterdogpe.event.defaults.FastTransferRequestEvent;
import dev.waterdog.waterdogpe.event.defaults.PostTransferCompleteEvent;
import dev.waterdog.waterdogpe.network.protocol.rewrite.types.RewriteData;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import dev.waterdog.waterdogpe.network.protocol.Signals;
import org.cloudburstmc.protocol.common.PacketSignal;

import dev.waterdog.waterdogpe.network.protocol.ProtocolVersion;

import static dev.waterdog.waterdogpe.network.protocol.Signals.mergeSignals;
import static dev.waterdog.waterdogpe.network.protocol.user.PlayerRewriteUtils.injectEntityImmobile;
import static dev.waterdog.waterdogpe.network.protocol.user.PlayerRewriteUtils.injectInputLocks;

public class ConnectedDownstreamHandler extends AbstractDownstreamHandler {

    public ConnectedDownstreamHandler(ProxiedPlayer player, ClientConnection connection) {
        super(player, connection);
    }

    @Override
    public PacketSignal handlePacket(BedrockPacket packet) {
        PacketSignal signal = super.handlePacket(packet);
        if (!player.getPluginPacketHandlers().isEmpty()) {
            for (PluginPacketHandler handler : this.player.getPluginPacketHandlers()) {
                signal = mergeSignals(signal, handler.handlePacket(packet, PacketDirection.CLIENT_BOUND));
            }
        }
        return signal;
    }

    @Override
    public PacketSignal handle(LevelChunkPacket packet) {
        // Remember whether this server serves chunks via the sub-chunk request system so injected
        // empty chunks match it and the client keeps requesting sub-chunks instead of breaking.
        this.player.setSubChunkRequestMode(packet.isRequestSubChunks());
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(TransferPacket packet) {
        if (!this.player.getProxy().getConfiguration().useFastTransfer()) {
            return PacketSignal.UNHANDLED;
        }

        ServerInfo serverInfo = this.player.getProxy().getServerInfo(packet.getAddress());
        if (serverInfo == null) {
            serverInfo = this.player.getProxy().getServerInfo(packet.getAddress(), packet.getPort());
        }

        FastTransferRequestEvent event = new FastTransferRequestEvent(serverInfo, this.player, packet.getAddress(), packet.getPort());
        this.player.getProxy().getEventManager().callEvent(event);

        if (!event.isCancelled() && event.getServerInfo() != null) {
            this.player.connect(event.getServerInfo());
            return Signals.CANCEL;
        }
        return PacketSignal.UNHANDLED;
    }

    @Override
    public final PacketSignal handle(DisconnectPacket packet) {
        // This is not really a failure
        this.player.onDownstreamFailure(this.connection, ReconnectReason.SERVER_KICK, packet.getKickMessage());
        return Signals.CANCEL;
    }
}
