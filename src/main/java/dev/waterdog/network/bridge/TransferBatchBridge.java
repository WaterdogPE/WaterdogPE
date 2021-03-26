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

package dev.waterdog.network.bridge;

import com.nukkitx.protocol.bedrock.BedrockPacket;
import com.nukkitx.protocol.bedrock.BedrockPacketType;
import com.nukkitx.protocol.bedrock.BedrockSession;
import com.nukkitx.protocol.bedrock.handler.BedrockPacketHandler;
import com.nukkitx.protocol.bedrock.packet.UnknownPacket;
import dev.waterdog.player.ProxiedPlayer;
import dev.waterdog.utils.exceptions.CancelSignalException;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class TransferBatchBridge extends ProxyBatchBridge {

    private final List<BedrockPacket> packetQueue = new ObjectArrayList<>();
    private final AtomicBoolean hasStartGame = new AtomicBoolean(false);

    public TransferBatchBridge(ProxiedPlayer player, BedrockSession session) {
        super(player, session);
        this.trackEntities = false;
    }

    @Override
    public void handle(BedrockSession session, ByteBuf buf, Collection<BedrockPacket> packets) {
        super.handle(session, buf, packets);

        // Send queued packets to upstream if new bridge is used
        if (this.hasStartGame.get() && (session.getBatchHandler() instanceof DownstreamBridge) && !this.packetQueue.isEmpty()) {
            this.session.sendWrapped(this.packetQueue, this.session.isEncrypted());
            this.packetQueue.clear();
        }
    }

    @Override
    public boolean handlePacket(BedrockPacket packet, BedrockPacketHandler handler) throws CancelSignalException {
        boolean isStartGame = packet.getPacketType() == BedrockPacketType.START_GAME;
        if (isStartGame) {
            this.hasStartGame.set(true);
        }
        super.handlePacket(packet, handler);

        // Packets after StartGamePacket should be queued
        // Ignore LevelEvent packet to prevent massive amounts of packets in queue
        if (!isStartGame && this.hasStartGame.get() && packet.getPacketType() != BedrockPacketType.LEVEL_EVENT) {
            this.packetQueue.add(ReferenceCountUtil.retain(packet));
        }
        throw CancelSignalException.CANCEL;
    }

    @Override
    public boolean handleUnknownPacket(UnknownPacket packet) {
        if (this.hasStartGame.get()) {
            this.packetQueue.add(packet.retain());
        }
        throw CancelSignalException.CANCEL;
    }

    public List<BedrockPacket> getPacketQueue() {
        return this.packetQueue;
    }
}
