/*
 * Copyright 2023 WaterdogTEAM
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

package dev.waterdog.waterdogpe.network.protocol.handler;

import dev.waterdog.waterdogpe.network.connection.ProxiedConnection;
import dev.waterdog.waterdogpe.network.protocol.PacketUtils;
import dev.waterdog.waterdogpe.network.protocol.Signals;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import io.netty.util.ReferenceCountUtil;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.protocol.bedrock.PacketDirection;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodecHelper;
import org.cloudburstmc.protocol.bedrock.codec.BedrockPacketDefinition;
import org.cloudburstmc.protocol.bedrock.data.PacketRecipient;
import org.cloudburstmc.protocol.bedrock.netty.BedrockBatchWrapper;
import org.cloudburstmc.protocol.bedrock.netty.BedrockPacketWrapper;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacketHandler;
import org.cloudburstmc.protocol.common.PacketSignal;
import org.cloudburstmc.protocol.common.util.Preconditions;

import java.util.ListIterator;

@Data
@Log4j2
public class ProxyBatchBridge implements BedrockPacketHandler {
    private final BedrockCodec codec;
    private final BedrockCodecHelper helper;

    private ProxyPacketHandler handler;
    private boolean forceEncode;
    private PacketDirection direction;
    /** Packet ids from downstream that already failed to decode, so each is logged once per connection. */
    private final IntSet failedPacketIds = new IntOpenHashSet();

    public ProxyBatchBridge(BedrockCodec codec, BedrockCodecHelper helper, ProxyPacketHandler handler, PacketDirection direction) {
        this.codec = codec;
        this.helper = helper;
        this.direction = direction;
        this.setHandler(handler);
    }

    public void onBedrockBatch(ProxiedConnection source, BedrockBatchWrapper batch) {
        ListIterator<BedrockPacketWrapper> iterator = batch.getPackets().listIterator();
        while (iterator.hasNext()) {
            BedrockPacketWrapper wrapper = iterator.next();
            boolean decoded = wrapper.getPacket() != null || this.decodePacket(source, wrapper);

            if (PacketUtils.TRACE_PACKETS) {
                BedrockPacket packet = wrapper.getPacket();
                // A packet bound for the server is one the client sent
                boolean fromClient = source.getPacketDirection().getInbound() == PacketRecipient.SERVER;
                log.info("[{} {}] {}", fromClient ? "client ->" : "server ->", source.getSocketAddress(),
                        packet == null ? "id " + wrapper.getPacketId() : packet.getPacketType());
            }

            if (!decoded) {
                // Forwarded untouched: the raw buffer is still in the batch
                continue;
            }

            PacketSignal signal = this.handlePacket(wrapper.getPacket());
            if (this.isForceEncode() || signal == PacketSignal.HANDLED) {
                ReferenceCountUtil.release(wrapper.getPacketBuffer());
                wrapper.setPacketBuffer(null); // clear cached buffer
                batch.modify();
            } else if (signal == Signals.CANCEL) {
                iterator.remove(); // remove from batch
                wrapper.release(); // release
                batch.modify();
            }
        }

        if (!batch.getPackets().isEmpty()) {
            this.sendProxiedBatch(batch);
        }
    }

    @Override
    public PacketSignal handlePacket(BedrockPacket packet) {
        try {
            PacketSignal signal = this.handler.handlePacket(packet);
            PacketSignal rewriteSignal = this.handler.doPacketRewrite(packet);
            if (this.direction.getInbound() == PacketRecipient.CLIENT) { // only track packets sent by downstream
                this.handler.getRewriteMaps().getEntityTracker().trackEntity(packet);
            }
            return Signals.mergeSignals(signal, rewriteSignal);
        } catch (Exception e) {
            throw new IllegalStateException("Error while handling " + packet.getPacketType(), e);
        }
    }

    /**
     * @return false if a downstream packet could not be decoded and is forwarded as is
     */
    private boolean decodePacket(ProxiedConnection source, BedrockPacketWrapper wrapper) {
        PacketRecipient inbound = source.getPacketDirection().getInbound();
        ByteBuf msg = wrapper.getPacketBuffer().retainedSlice();
        try {
            msg.skipBytes(wrapper.getHeaderLength()); // skip header
            wrapper.setPacket(this.codec.tryDecode(helper, msg, wrapper.getPacketId(), inbound));
            return true;
        } catch (Exception e) {
            // Client packets stay strict. A server packet we can't read is only needed for rewrites, so dropping
            // the connection over it would cost far more than skipping the rewrite.
            if (inbound != PacketRecipient.CLIENT) {
                log.warn("Failed to decode packet", e);
                throw e;
            }
            if (this.failedPacketIds.add(wrapper.getPacketId())) {
                log.warn("[{}] Could not decode {} from downstream, forwarding it without rewrites",
                        source.getSocketAddress(), this.packetName(wrapper.getPacketId()), e);
            }
            return false;
        } finally {
            msg.release();
        }
    }

    private String packetName(int packetId) {
        BedrockPacketDefinition<? extends BedrockPacket> definition = this.codec.getPacketDefinition(packetId);
        return definition == null ? "packet " + packetId : definition.getFactory().get().getClass().getSimpleName();
    }

    public void sendProxiedBatch(BedrockBatchWrapper batch) {
        this.handler.sendProxiedBatch(batch);
    }

    public void setHandler(ProxyPacketHandler handler) {
        Preconditions.checkNotNull(handler, "Handler can not be null");
        this.handler = handler;
    }
}
