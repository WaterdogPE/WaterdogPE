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

import com.nukkitx.protocol.bedrock.handler.BedrockPacketHandler;
import com.nukkitx.protocol.bedrock.packet.ClientCacheStatusPacket;
import com.nukkitx.protocol.bedrock.packet.PacketViolationWarningPacket;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import dev.waterdog.waterdogpe.utils.exceptions.CancelSignalException;

public class AbstractUpstreamHandler implements BedrockPacketHandler {

    protected final ProxiedPlayer player;

    public AbstractUpstreamHandler(ProxiedPlayer player) {
        this.player = player;
    }

    @Override
    public boolean handle(ClientCacheStatusPacket packet) {
        this.player.getLoginData().setCachePacket(packet);
        return this.cancel();
    }

    @Override
    public final boolean handle(PacketViolationWarningPacket packet) {
        this.player.getLogger().warning("Received violation from " + this.player.getName() + ": " + packet.toString());
        return this.cancel();
    }

    /**
     * If connection has bridge we cancel packet to prevent sending it to downstream.
     * @return true is we can't use CancelSignalException.
     */
    protected boolean cancel() {
        if (this.player.hasUpstreamBridge()) {
            throw CancelSignalException.CANCEL;
        }
        return true;
    }
}
