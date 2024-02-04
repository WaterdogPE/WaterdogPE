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
import dev.waterdog.waterdogpe.network.protocol.Signals;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodecHelper;
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

    public ProxyBatchBridge(BedrockCodec codec, BedrockCodecHelper helper, ProxyPacketHandler handler) {
        this.codec = codec;
        this.helper = helper;
        this.setHandler(handler);
    }

    public void onBedrockBatch(ProxiedConnection source, BedrockBatchWrapper batch) {
        ListIterator<BedrockPacketWrapper> iterator = batch.getPackets().listIterator();
        while (iterator.hasNext()) {
            BedrockPacketWrapper wrapper = iterator.next();
            if (wrapper.getPacket() == null) {
                this.decodePacket(wrapper);
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
            this.handler.getRewriteMaps().getEntityTracker().trackEntity(packet);
            return Signals.mergeSignals(signal, rewriteSignal);
        } catch (Exception e) {
            throw new IllegalStateException("Error while handling " + packet.getPacketType(), e);
        }
    }

    private void decodePacket(BedrockPacketWrapper wrapper) {
        ByteBuf msg = wrapper.getPacketBuffer().retainedSlice();
        try {
            msg.skipBytes(wrapper.getHeaderLength()); // skip header
            wrapper.setPacket(this.codec.tryDecode(helper, msg, wrapper.getPacketId()));
        } catch (Throwable t) {
            log.warn("Failed to decode packet", t);
            throw t;
        } finally {
            msg.release();
        }
    }

    public void sendProxiedBatch(BedrockBatchWrapper batch) {
        this.handler.sendProxiedBatch(batch);
    }

    public void setHandler(ProxyPacketHandler handler) {
        Preconditions.checkNotNull(handler, "Handler can not be null");
        this.handler = handler;
    }
}
