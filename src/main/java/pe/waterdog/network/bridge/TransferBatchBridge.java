/**
 * Copyright 2020 WaterdogTEAM
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pe.waterdog.network.bridge;

import com.nukkitx.protocol.bedrock.BedrockPacket;
import com.nukkitx.protocol.bedrock.BedrockPacketType;
import com.nukkitx.protocol.bedrock.BedrockSession;
import com.nukkitx.protocol.bedrock.handler.BedrockPacketHandler;
import com.nukkitx.protocol.bedrock.packet.UnknownPacket;
import io.netty.buffer.ByteBuf;
import pe.waterdog.player.ProxiedPlayer;
import pe.waterdog.utils.exceptions.CancelSignalException;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class TransferBatchBridge extends ProxyBatchBridge {

    private final Queue<BedrockPacket> packetQueue = new LinkedList<>();
    private final AtomicBoolean hasStartGame = new AtomicBoolean(false);

    public TransferBatchBridge(ProxiedPlayer player, BedrockSession session) {
        super(player, session);
        this.trackEntities = false;
    }

    @Override
    public void handle(BedrockSession session, ByteBuf buf, Collection<BedrockPacket> packets) {
        super.handle(session, buf, packets);

        // Send queued packets to upstream if new bridge is used
        if (this.hasStartGame.get() && (session.getBatchHandler() instanceof DownstreamBridge) && !this.packetQueue.isEmpty()){
            this.session.sendWrapped(this.packetQueue, this.session.isEncrypted());
        }
    }

    @Override
    public boolean handlePacket(BedrockPacket packet, BedrockPacketHandler handler) throws CancelSignalException {
        boolean isStartGame = packet.getPacketType() == BedrockPacketType.START_GAME;
        if (isStartGame){
            this.hasStartGame.set(true);
        }
        super.handlePacket(packet, handler);

        // Packets after StartGamePacket should be queued
        // Ignore LevelEvent packet to prevent massive amounts of packets in queue
        if (!isStartGame && this.hasStartGame.get() && packet.getPacketType() != BedrockPacketType.LEVEL_EVENT){
            this.packetQueue.add(packet);
        }
        throw CancelSignalException.CANCEL;
    }

    @Override
    public boolean handleUnknownPacket(UnknownPacket packet) {
        int refCnt = packet.refCnt();
        if (refCnt > 0){
            packet.release(refCnt);
        }
        throw CancelSignalException.CANCEL;
    }

    public Queue<BedrockPacket> getPacketQueue() {
        return this.packetQueue;
    }
}
