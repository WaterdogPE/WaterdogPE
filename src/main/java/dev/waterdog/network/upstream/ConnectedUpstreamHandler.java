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

package dev.waterdog.network.upstream;

import com.nukkitx.protocol.bedrock.data.PlayerActionType;
import com.nukkitx.protocol.bedrock.packet.*;
import dev.waterdog.ProxyServer;
import dev.waterdog.event.defaults.PlayerChatEvent;
import dev.waterdog.network.rewrite.types.RewriteData;
import dev.waterdog.network.session.TransferCallback;
import dev.waterdog.player.ProxiedPlayer;
import dev.waterdog.utils.exceptions.CancelSignalException;

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
    public boolean handle(PlayerActionPacket packet) {
        if (this.player.getDimensionChangeState() < 1 || packet.getAction() != PlayerActionType.DIMENSION_CHANGE_SUCCESS) {
            return false;
        }

        RewriteData rewriteData = this.player.getRewriteData();
        TransferCallback transferCallback = rewriteData.getTransferCallback();
        int dimChangeState = this.player.getDimensionChangeState();
        if (dimChangeState == 1 && transferCallback != null) {
            // First dimension change was completed successfully.
            transferCallback.onTransferAccepted();
            this.player.setDimensionChangeState(2);
            throw CancelSignalException.CANCEL;
        }

        if (dimChangeState == 2 && transferCallback != null) {
            // At this point dimension change sequence was completed.
            // We can finally fully initialize connection.
            transferCallback.onTransferComplete();
            this.player.setDimensionChangeState(0);
            throw CancelSignalException.CANCEL;
        }
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
