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
import com.nukkitx.protocol.bedrock.handler.BatchHandler;
import com.nukkitx.protocol.bedrock.handler.BedrockPacketHandler;
import dev.waterdog.waterdogpe.network.session.CompressionAlgorithm;
import dev.waterdog.waterdogpe.network.session.DownstreamSession;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import dev.waterdog.waterdogpe.utils.exceptions.CancelSignalException;
import dev.waterdog.waterdogpe.utils.types.PacketHandler;
import io.netty.buffer.ByteBuf;

import java.util.Collection;

/**
 * This is the default upstream to downstream implementation of BatchBridge which is used during all life cycles of the connection.
 * UpstreamBridge is assigned by the proxy automatically on the initial connection or at the end of the transfer phase when upstream packets are
 * allowed to be sent to downstream. Note that each implementation of DownstreamSession must be capable of handling raw ByteBuf and Collection<BedrockPacket>
 * from sendWrapped() if this.forceEncodePackets is disabled.
 * Decoded packets are from here passed to rewrite maps and optionaly to plugin handler.
 */
public class UpstreamBridge extends ProxyBatchBridge implements BatchHandler {

    private final DownstreamSession session;

    public UpstreamBridge(ProxiedPlayer player, DownstreamSession session) {
        super(player);
        this.session = session;
    }

    @Override
    public void handle(BedrockSession session, ByteBuf byteBuf, Collection<BedrockPacket> packets) {
        this.handle(session.getPacketHandler(), byteBuf, packets, this.player.getUpstreamCompression());
    }

    @Override
    public void sendWrapped(Collection<BedrockPacket> packets, boolean encrypt) {
        this.session.sendWrapped(packets, encrypt);
    }

    @Override
    public void sendWrapped(ByteBuf compressed, boolean encrypt) {
        this.session.sendWrapped(compressed, encrypt);
    }

    @Override
    public boolean isEncrypted() {
        return this.session.isEncrypted();
    }

    @Override
    public CompressionAlgorithm getCompression() {
        return this.session.getCompression();
    }

    @Override
    public boolean handlePacket(BedrockPacket packet, BedrockPacketHandler handler) throws CancelSignalException {
        boolean changed = super.handlePacket(packet, handler);
        boolean pluginHandled = false;
        if (!this.player.getPluginUpstreamHandlers().isEmpty()) {
            for (PacketHandler pluginHandler : this.player.getPluginUpstreamHandlers()) {
                if (pluginHandler.handlePacket(packet)) {
                    pluginHandled = true;
                }
            }
        }
        return changed || pluginHandled;
    }
}
