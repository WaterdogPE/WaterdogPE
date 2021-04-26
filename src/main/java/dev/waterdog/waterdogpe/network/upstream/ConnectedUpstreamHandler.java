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

package dev.waterdog.waterdogpe.network.upstream;

import com.nukkitx.protocol.bedrock.packet.*;
import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.event.defaults.PlayerChatEvent;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import dev.waterdog.waterdogpe.utils.exceptions.CancelSignalException;

/**
 * Main handler for handling packets received from upstream.
 */
public class ConnectedUpstreamHandler extends AbstractUpstreamHandler {

    public ConnectedUpstreamHandler(ProxiedPlayer player) {
        super(player);
    }

    @Override
    public boolean handle(ClientCacheStatusPacket packet) {
        this.player.getLoginData().setCachePacket(packet);
        return this.cancel();
    }

    @Override
    public final boolean handle(RequestChunkRadiusPacket packet) {
        this.player.getLoginData().setChunkRadius(packet);
        return false;
    }

    @Override
    public final boolean handle(TextPacket packet) {
        PlayerChatEvent event = new PlayerChatEvent(this.player, packet.getMessage());
        ProxyServer.getInstance().getEventManager().callEvent(event);
        packet.setMessage(event.getMessage());

        if (event.isCancelled()) {
            throw CancelSignalException.CANCEL;
        }
        return true;
    }

    @Override
    public final boolean handle(CommandRequestPacket packet) {
        String message = packet.getCommand();
        if (this.player.getProxy().handlePlayerCommand(this.player, message)) {
            throw CancelSignalException.CANCEL;
        }
        return false;
    }
}
