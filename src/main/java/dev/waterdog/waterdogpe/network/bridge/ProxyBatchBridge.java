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

import com.nukkitx.protocol.bedrock.handler.BedrockPacketHandler;
import com.nukkitx.protocol.bedrock.packet.UnknownPacket;
import dev.waterdog.waterdogpe.network.session.CompressionAlgorithm;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import dev.waterdog.waterdogpe.utils.exceptions.CancelSignalException;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.Collection;
import java.util.List;

/**
 * This is the class where things get interesting.
 * BatchBridge classes are responsible for re-encoding and resending packets from upstream to downstream and vice-versa.
 * WARNING: This class includes more tricks which might be harder to understand.
 * Do NOT touch anything here unless you understand what you are doing and are aware of all consequences!
 */
public abstract class ProxyBatchBridge {

    protected final ProxiedPlayer player;
    protected volatile boolean trackEntities = true;
    protected volatile boolean forceEncodePackets = false;

    public ProxyBatchBridge(ProxiedPlayer player) {
        this.player = player;
    }

    /**
     * Handle packets and do all the logic to forward packets
     * @param handler handler of the session which sent the packets
     * @param compressed buffer with compressed data
     * @param packets decoded packets of buffer
     * @param compression compression used for compressed buffer
     */
    public void handle(BedrockPacketHandler handler, ByteBuf compressed, Collection<BedrockPacket> packets, CompressionAlgorithm compression) {
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

        if (changed) {
            player.getProxy().getMetricsHandler().changedBatch();
        } else {
            player.getProxy().getMetricsHandler().unchangedBatch();
        }

        if (this.forceEncodePackets || compression != this.getCompression() || !allPackets.isEmpty() && (changed || allPackets.size() != packets.size())) {
            this.sendWrapped(allPackets, this.isEncrypted());
            return;
        }

        if (!changed && allPackets.size() == packets.size()) {
            compressed.resetReaderIndex(); // Set reader index to position where payload is decrypted.
            this.sendWrapped(compressed, this.isEncrypted());
        }

        // Packets from array aren't used so we can deallocate whole.
        this.deallocatePackets(allPackets);
    }

    public abstract void sendWrapped(Collection<BedrockPacket> packets, boolean encrypt);
    public abstract void sendWrapped(ByteBuf compressed, boolean encrypt);

    public abstract boolean isEncrypted();

    public abstract CompressionAlgorithm getCompression();

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
