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

package dev.waterdog.waterdogpe.network.connection.peer;

import dev.waterdog.waterdogpe.network.connection.ProxiedConnection;
import dev.waterdog.waterdogpe.network.protocol.Signals;
import dev.waterdog.waterdogpe.network.protocol.handler.ProxyBatchBridge;
import dev.waterdog.waterdogpe.network.protocol.handler.ProxyPacketHandler;
import dev.waterdog.waterdogpe.network.connection.codec.BedrockBatchWrapper;
import lombok.extern.log4j.Log4j2;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.protocol.bedrock.BedrockSession;
import org.cloudburstmc.protocol.bedrock.netty.BedrockPacketWrapper;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacketHandler;
import org.cloudburstmc.protocol.common.PacketSignal;

import java.util.function.Consumer;

@Log4j2
public abstract class ProxiedBedrockSession extends BedrockSession implements ProxiedConnection {

    public ProxiedBedrockSession(ProxiedBedrockPeer peer, int subClientId) {
        super(peer, subClientId);
    }

    public void sendPacket(BedrockBatchWrapper batch) {
        this.getPeer().sendPacket(batch);
    }

    @Override
    protected void onPacket(BedrockPacket packet) {
        if (this.packetHandler instanceof ProxyBatchBridge bridge) {
            PacketSignal signal = bridge.handlePacket(packet);
            if (signal != Signals.CANCEL) {
                bridge.sendProxiedBatch(BedrockBatchWrapper.create(0, packet));
            }
        } else if (this.packetHandler != null) {
            this.getPacketHandler().handlePacket(packet);
        } else {
            log.warn("[{}] Unhandled packet {}", this.getSocketAddress(), packet);
        }
    }

    protected void onBedrockBatch(BedrockBatchWrapper batch) {
        if (this.packetHandler instanceof ProxyBatchBridge bridge) {
            bridge.onBedrockBatch(this, batch);
        } else {
            for (BedrockPacketWrapper packet : batch.getPackets()) {
                this.onPacket(packet.getPacket());
            }
        }
    }

    public int getSubClientId() {
        return this.subClientId;
    }

    public void addDisconnectListener(Consumer<String> listener) {
        this.getPeer().getChannel().closeFuture().addListener(future -> listener.accept(this.getDisconnectReason()));
    }

    @Override
    public void setPacketHandler(@NonNull BedrockPacketHandler handler) {
        if (handler instanceof ProxyPacketHandler packetHandler) {
            if (this.packetHandler instanceof ProxyBatchBridge bridge) {
                bridge.setHandler(packetHandler);
            } else {
                super.setPacketHandler(new ProxyBatchBridge(this.getPeer().getCodec(),
                        this.getPeer().getCodecHelper(), packetHandler));
            }
        } else {
            super.setPacketHandler(handler);
        }
    }

    @Override
    public BedrockPacketHandler getPacketHandler() {
        if (this.packetHandler instanceof ProxyBatchBridge bridge) {
            return bridge.getHandler();
        }
        return this.packetHandler;
    }

    @Override
    public ProxiedBedrockPeer getPeer() {
        return (ProxiedBedrockPeer) super.getPeer();
    }

    @Override
    public long getPing() {
        return this.getPeer().getPing();
    }
}
