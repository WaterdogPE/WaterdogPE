/*
 * Copyright 2022 WaterdogTEAM
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
import dev.waterdog.waterdogpe.network.connection.codec.BedrockBatchWrapper;
import dev.waterdog.waterdogpe.network.protocol.Signals;
import dev.waterdog.waterdogpe.network.protocol.rewrite.RewriteMaps;
import io.netty.util.ReferenceCountUtil;
import org.cloudburstmc.protocol.bedrock.netty.BedrockPacketWrapper;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacketHandler;
import org.cloudburstmc.protocol.common.PacketSignal;

import java.util.ListIterator;

public interface ProxyBatchBridge extends BedrockPacketHandler {
    void sendProxiedBatch(BedrockBatchWrapper batch);

    ProxiedConnection getConnection();
    RewriteMaps getRewriteMaps();

    default boolean isForceEncode() {
        return false;
    }

    default void onBedrockBatch(ProxiedConnection source, BedrockBatchWrapper batch) {
        ListIterator<BedrockPacketWrapper> iterator = batch.getPackets().listIterator();
        while (iterator.hasNext()) {
            BedrockPacketWrapper wrapper = iterator.next();
            PacketSignal signal = this.handlePacket(source, wrapper.getPacket());
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

    default PacketSignal handlePacket(ProxiedConnection source, BedrockPacket packet) {
        try {
            PacketSignal signal = this.handlePacket(packet);
            PacketSignal rewriteSignal = this.doPacketRewrite(packet);
            this.getRewriteMaps().getEntityTracker().trackEntity(packet);
            return Signals.mergeSignals(signal, rewriteSignal);
        } catch (Exception e) {
            throw new IllegalStateException("Error while handling " + packet.getPacketType(), e);
        }
    }

    default PacketSignal doPacketRewrite(BedrockPacket packet) {
        return this.getRewriteMaps().getEntityMap().doRewrite(packet);
    }
}
