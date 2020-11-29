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
import com.nukkitx.protocol.bedrock.BedrockSession;
import com.nukkitx.protocol.bedrock.handler.BatchHandler;
import com.nukkitx.protocol.bedrock.handler.BedrockPacketHandler;
import com.nukkitx.protocol.bedrock.packet.UnknownPacket;
import io.netty.buffer.ByteBuf;
import pe.waterdog.network.protocol.ProtocolVersion;
import pe.waterdog.player.ProxiedPlayer;
import pe.waterdog.utils.exceptions.CancelSignalException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class ProxyBatchBridge implements BatchHandler {

    protected final BedrockSession session;
    protected final ProxiedPlayer player;

    protected boolean trackEntities = true;

    public ProxyBatchBridge(ProxiedPlayer player, BedrockSession session) {
        this.session = session;
        this.player = player;
    }

    @Override
    public void handle(BedrockSession session, ByteBuf buf, Collection<BedrockPacket> packets) {
        BedrockPacketHandler handler = session.getPacketHandler();
        List<BedrockPacket> allPackets = new ArrayList<>();
        boolean changed = false;

        for (BedrockPacket packet : packets) {
            try {
                if ((packet instanceof UnknownPacket) && this.handleUnknownPacket((UnknownPacket) packet) ||
                        !(packet instanceof UnknownPacket) && this.handlePacket(packet, handler)) {
                    changed = true;
                }
                allPackets.add(packet);
            } catch (CancelSignalException e) {
            }
        }

        if (!changed && allPackets.size() == packets.size()) {
            buf.readerIndex(1);
            this.session.sendWrapped(buf, this.session.isEncrypted());
        } else if (!allPackets.isEmpty()){
            this.session.sendWrapped(allPackets, true);
        }
    }

    /**
     * @return if packet was changed
     * @throws CancelSignalException if we do not want to send packet
     */
    public boolean handlePacket(BedrockPacket packet, BedrockPacketHandler handler) throws CancelSignalException {
        boolean handled = false, canceled = false;
        try {
            handled = packet.handle(handler);
        } catch (CancelSignalException e) {
            canceled = true;
        }

        boolean changed = this.player.getEntityMap().doRewrite(packet) || handled;
        if (!changed && canceled) {
            throw CancelSignalException.CANCEL;
        }

        if (this.trackEntities) {
            this.player.getEntityTracker().trackEntity(packet);
        }
        return changed;
    }

    public boolean handleUnknownPacket(UnknownPacket packet){
        return false;
    }
}
