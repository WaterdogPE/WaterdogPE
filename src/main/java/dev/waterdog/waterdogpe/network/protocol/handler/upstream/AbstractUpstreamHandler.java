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

import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.event.defaults.PlayerPacketViolationEvent;
import dev.waterdog.waterdogpe.event.defaults.PostTransferCompleteEvent;
import dev.waterdog.waterdogpe.event.defaults.UpstreamPacketReceivedEvent;
import dev.waterdog.waterdogpe.event.defaults.UpstreamPacketSentEvent;
import dev.waterdog.waterdogpe.network.protocol.Signals;
import dev.waterdog.waterdogpe.network.protocol.handler.PluginPacketHandler;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import org.cloudburstmc.protocol.bedrock.PacketDirection;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacketHandler;
import org.cloudburstmc.protocol.bedrock.packet.ClientCacheStatusPacket;
import org.cloudburstmc.protocol.bedrock.packet.PacketViolationWarningPacket;
import org.cloudburstmc.protocol.common.PacketSignal;

import static dev.waterdog.waterdogpe.network.protocol.Signals.mergeSignals;

public abstract class AbstractUpstreamHandler implements BedrockPacketHandler {

    protected final ProxiedPlayer player;

    public AbstractUpstreamHandler(ProxiedPlayer player) {
        this.player = player;
    }

    @Override
    public PacketSignal handlePacket(BedrockPacket packet) {
        PacketSignal signal = BedrockPacketHandler.super.handlePacket(packet);
        if (player.getPluginPacketHandlers().size() > 0) {
            for (PluginPacketHandler handler : this.player.getPluginPacketHandlers()) {
                signal = mergeSignals(signal, handler.handlePacket(packet, PacketDirection.SERVER_BOUND));
            }
        }

        UpstreamPacketReceivedEvent event = new UpstreamPacketReceivedEvent(player, packet);
        ProxyServer.getInstance().getEventManager().callEvent(event);
        if(event.isCancelled()) return this.cancel();
        return signal;
    }

    @Override
    public PacketSignal handle(ClientCacheStatusPacket packet) {
        this.player.getLoginData().setCachePacket(packet);
        return this.cancel();
    }

    @Override
    public final PacketSignal handle(PacketViolationWarningPacket packet) {
        this.player.getLogger().warning("Received violation from " + this.player.getName() + ": " + packet.toString());

        this.player.getProxy().getEventManager().callEvent(
                new PlayerPacketViolationEvent(this.player, packet)
        );

        return this.cancel();
    }

    /**
     * If connection has bridge we cancel packet to prevent sending it to downstream.
     * @return true is we can't use CancelSignalException.
     */
    protected PacketSignal cancel() {
        if (this.player.hasUpstreamBridge()) {
            return Signals.CANCEL;
        }
        return PacketSignal.HANDLED;
    }
}
