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

package dev.waterdog.waterdogpe.network.bridge;

import com.nukkitx.protocol.bedrock.BedrockPacket;
import com.nukkitx.protocol.bedrock.BedrockSession;
import com.nukkitx.protocol.bedrock.handler.BedrockPacketHandler;
import dev.waterdog.waterdogpe.network.rewrite.RewriteMaps;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import dev.waterdog.waterdogpe.utils.exceptions.CancelSignalException;
import dev.waterdog.waterdogpe.utils.types.PacketHandler;

/**
 * This is the downstream implementation of the {@link AbstractDownstreamBatchBridge} which is used after initial connection initialization or
 * after transfer process is completed.
 * From here decoded maps are passed to the rewrite maps and optionally the plugin handlers.
 */
public class DownstreamBridge extends AbstractDownstreamBatchBridge {

    public DownstreamBridge(ProxiedPlayer player, BedrockSession upstreamSession) {
        super(player, upstreamSession);
    }

    @Override
    public boolean handlePacket(BedrockPacket packet, BedrockPacketHandler handler) throws CancelSignalException {
        boolean changed = super.handlePacket(packet, handler);

        RewriteMaps rewriteMaps = this.player.getRewriteMaps();
        boolean rewroteBlock = rewriteMaps.getBlockMap() != null && rewriteMaps.getBlockMap().doRewrite(packet);

        boolean pluginHandled = false;
        if (!this.player.getPluginDownstreamHandlers().isEmpty()) {
            for (PacketHandler pluginHandler : this.player.getPluginDownstreamHandlers()) {
                if (pluginHandler.handlePacket(packet)) {
                    pluginHandled = true;
                }
            }
        }
        return changed || rewroteBlock || pluginHandled;
    }
}
