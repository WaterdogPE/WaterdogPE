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
import com.nukkitx.protocol.bedrock.BedrockSession;
import com.nukkitx.protocol.bedrock.handler.BatchHandler;
import com.nukkitx.protocol.bedrock.handler.BedrockPacketHandler;
import com.nukkitx.protocol.bedrock.packet.UnknownPacket;
import dev.waterdog.player.ProxiedPlayer;
import dev.waterdog.utils.exceptions.CancelSignalException;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

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
        List<BedrockPacket> allPackets = new ObjectArrayList<>();
        boolean changed = false;

        for (BedrockPacket packet : packets) {
            try {
                if ((packet instanceof UnknownPacket) && this.handleUnknownPacket((UnknownPacket) packet) ||
                        !(packet instanceof UnknownPacket) && this.handlePacket(packet, handler)) {
                    changed = true;
                }
                allPackets.add(packet);
            } catch (CancelSignalException e) {
                // In this case packet won't be released by protocol lib
                ReferenceCountUtil.release(packet);
            }
        }

        if (!allPackets.isEmpty() && (changed || allPackets.size() != packets.size())) {
            this.session.sendWrapped(allPackets, this.session.isEncrypted());
            return;
        }

        if (!changed && allPackets.size() == packets.size()) {
            buf.resetReaderIndex(); // Set reader index to position where payload is decrypted.
            this.session.sendWrapped(buf, this.session.isEncrypted());
        }

        // Packets from array aren't used so we can deallocate whole.
        this.deallocatePackets(allPackets);
    }

    protected void deallocatePackets(Collection<BedrockPacket> packets) {
        for (BedrockPacket packet : packets) {
            int refCnt = ReferenceCountUtil.refCnt(packet);
            if (refCnt > 0) {
                ReferenceCountUtil.release(packet);
            }
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

        boolean changed = this.player.getRewriteMaps().getEntityMap().doRewrite(packet) || handled;
        if (!changed && canceled) {
            throw CancelSignalException.CANCEL;
        }

        if (this.trackEntities) {
            this.player.getRewriteMaps().getEntityTracker().trackEntity(packet);
        }
        return changed;
    }

    public boolean handleUnknownPacket(UnknownPacket packet) {
        return false;
    }
}
