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

package dev.waterdog.waterdogpe.network.session.bedrock;

import com.nukkitx.protocol.bedrock.BedrockPacket;
import com.nukkitx.protocol.bedrock.BedrockSession;
import com.nukkitx.protocol.bedrock.handler.BatchHandler;
import dev.waterdog.waterdogpe.network.bridge.TransferBatchBridge;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import io.netty.buffer.ByteBuf;

import java.util.Collection;

public class BedrockTransferBatchBridge extends TransferBatchBridge implements BatchHandler {

    private final BedrockDefaultSession session;

    public BedrockTransferBatchBridge(ProxiedPlayer player, BedrockSession upstreamSession, BedrockDefaultSession session) {
        super(player, upstreamSession);
        this.session = session;
    }

    @Override
    public void handle(BedrockSession bedrockSession, ByteBuf byteBuf, Collection<BedrockPacket> packets) {
        this.handle(bedrockSession.getPacketHandler(), byteBuf, packets, this.session.getCompression());
    }

    @Override
    public void flushQueue() {
        if (this.session.getSession().getEventLoop().inEventLoop()) {
            super.flushQueue();
        } else {
            this.session.getSession().getEventLoop().execute(super::flushQueue);
        }
    }

    @Override
    public void free() {
        if (this.session.getSession().getEventLoop().inEventLoop()) {
            super.free();
        } else {
            this.session.getSession().getEventLoop().execute(super::free);
        }
    }
}
