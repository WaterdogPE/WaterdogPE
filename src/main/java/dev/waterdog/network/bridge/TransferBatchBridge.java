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

import com.nukkitx.network.raknet.RakNetSession;
import com.nukkitx.protocol.bedrock.BedrockPacket;
import com.nukkitx.protocol.bedrock.BedrockPacketType;
import com.nukkitx.protocol.bedrock.BedrockSession;
import com.nukkitx.protocol.bedrock.handler.BedrockPacketHandler;
import com.nukkitx.protocol.bedrock.packet.UnknownPacket;
import dev.waterdog.utils.exceptions.CancelSignalException;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import dev.waterdog.player.ProxiedPlayer;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.*;

public class TransferBatchBridge extends ProxyBatchBridge {

    private final List<BedrockPacket> packetQueue = new ObjectArrayList<>();
    private volatile boolean hasStartGame = false;
    private volatile boolean dimLockActive = true;

    public TransferBatchBridge(ProxiedPlayer player, BedrockSession session) {
        super(player, session);
    }

    @Override
    public void handle(BedrockSession session, ByteBuf buf, Collection<BedrockPacket> packets) {
        super.handle(session, buf, packets);
        if (this.hasStartGame && !this.dimLockActive) {
            // Send queued packets to upstream if dim lock is disabled
            this.flushQueue(session);
        }
    }

    /**
     * Here we send all queued packets from downstream to upstream.
     * Packets will be sent after StartGamePacket is received and dimension change sequence has been passed.
     * @param downstream instance of BedrockSession which is this handler assigned to.
     */
    public void flushQueue(BedrockSession downstream) {
        RakNetSession rakSession = (RakNetSession) downstream.getConnection();
        if (rakSession.getEventLoop().inEventLoop()) {
            this.flushQueue0();
        } else {
            rakSession.getEventLoop().execute(this::flushQueue0);
        }
    }

    private void flushQueue0() {
        Collection<BedrockPacket> outboundQueue = new ObjectArrayList<>();
        if (this.packetQueue.isEmpty()) {
            return;
        }
        outboundQueue.addAll(this.packetQueue);
        this.packetQueue.clear();

        if (outboundQueue.size() >= 512) {
            // TODO: consider closing connection or splitting to more batches
            this.player.getLogger().warning("TransferBatchBridge packet queue is too large! Got "+outboundQueue.size()+" packets with "+this.player.getName());
        }

        this.session.sendWrapped(outboundQueue, this.session.isEncrypted());
    }

    @Override
    public boolean handlePacket(BedrockPacket packet, BedrockPacketHandler handler) throws CancelSignalException {
        boolean isStartGame = packet.getPacketType() == BedrockPacketType.START_GAME;
        if (isStartGame){
            this.hasStartGame = true;
        }
        super.handlePacket(packet, handler);

        // Packets after StartGamePacket should be queued
        // Ignore LevelEvent packet to prevent massive amounts of packets in queue
        if (!isStartGame && this.hasStartGame && packet.getPacketType() != BedrockPacketType.LEVEL_EVENT) {
            this.packetQueue.add(ReferenceCountUtil.retain(packet));
        }
        throw CancelSignalException.CANCEL;
    }

    @Override
    public boolean handleUnknownPacket(UnknownPacket packet) {
        if (this.hasStartGame) {
            this.packetQueue.add(packet.retain());
        }
        throw CancelSignalException.CANCEL;
    }

    public void setDimLockActive(boolean active) {
        this.dimLockActive = active;
    }

    public boolean isDimLockActive() {
        return this.dimLockActive;
    }
}
