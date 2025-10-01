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
import dev.waterdog.waterdogpe.network.connection.codec.batch.BatchFlags;
import dev.waterdog.waterdogpe.network.connection.codec.server.PacketQueueHandler;
import dev.waterdog.waterdogpe.network.protocol.Signals;
import dev.waterdog.waterdogpe.network.protocol.handler.ProxyBatchBridge;
import dev.waterdog.waterdogpe.network.protocol.handler.ProxyPacketHandler;
import io.netty.channel.ChannelPipeline;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.protocol.bedrock.BedrockDisconnectReasons;
import org.cloudburstmc.protocol.bedrock.BedrockPeer;
import org.cloudburstmc.protocol.bedrock.BedrockSession;
import org.cloudburstmc.protocol.bedrock.PacketDirection;
import org.cloudburstmc.protocol.bedrock.netty.BedrockBatchWrapper;
import org.cloudburstmc.protocol.bedrock.netty.BedrockPacketWrapper;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacketHandler;
import org.cloudburstmc.protocol.bedrock.packet.DisconnectPacket;
import org.cloudburstmc.protocol.common.PacketSignal;

import java.util.function.Consumer;

@Log4j2
public class BedrockServerSession extends BedrockSession implements ProxiedConnection {

    public BedrockServerSession(ProxiedBedrockPeer peer, int subClientId) {
        super(peer, subClientId);
    }

    @Override
    protected void onPacket(BedrockPacketWrapper packet) {
        if (this.packetHandler instanceof ProxyBatchBridge bridge) {
            PacketSignal signal = bridge.handlePacket(packet.getPacket());
            if (signal != Signals.CANCEL) {
                bridge.sendProxiedBatch(BedrockBatchWrapper.create(0, packet.getPacket()));
            }
        } else if (this.packetHandler != null) {
            this.getPacketHandler().handlePacket(packet.getPacket());
        } else {
            log.warn("[{}] Unhandled packet {}", this.getSocketAddress(), packet);
        }
    }

    protected void onBedrockBatch(BedrockBatchWrapper batch) {
        if (this.packetHandler instanceof ProxyBatchBridge bridge) {
            bridge.onBedrockBatch(this, batch);
        } else {
            batch.getPackets().forEach(this::onPacket);
        }
    }

    public void sendPacket(BedrockBatchWrapper batch) {
        this.getPeer().sendPacket(batch);
    }

    @Override
    public void sendPacketImmediately(BedrockPacket packet) {
        BedrockBatchWrapper batch = BedrockBatchWrapper.create(this.subClientId, packet);
        batch.setFlag(BatchFlags.SKIP_QUEUE);
        this.getPeer().sendPacket(batch);
    }

    @Override
    public void disconnect(CharSequence reason, boolean hideReason) {
        this.checkForClosed();

        DisconnectPacket packet = new DisconnectPacket();
        if (reason == null || hideReason) {
            packet.setMessageSkipped(true);
            reason = BedrockDisconnectReasons.DISCONNECTED;
        }
        packet.setKickMessage(reason);
        this.sendPacketImmediately(packet);
    }

    public void setTransferQueueActive(boolean enable) {
        if (!this.getPeer().isConnected()) {
            throw new IllegalStateException("Connection was already closed");
        }

        ChannelPipeline pipeline = this.getPeer().getChannel().pipeline();
        if (enable) {
            if (pipeline.get(PacketQueueHandler.NAME) == null) {
                pipeline.addBefore(BedrockPeer.NAME, PacketQueueHandler.NAME, new PacketQueueHandler(this));
            } else {
                throw new IllegalStateException("Transfer queue for " + this.getSocketAddress() + " is already active");
            }
        } else if (pipeline.get(PacketQueueHandler.NAME) != null) {
            pipeline.remove(PacketQueueHandler.NAME);
        }
    }

    @Override
    public void setPacketHandler(BedrockPacketHandler handler) {
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

    public void addDisconnectListener(Consumer<CharSequence> listener) {
        this.getPeer().getChannel().closeFuture().addListener(future -> {
            listener.accept(this.getDisconnectReason());
        });
    }

    public int getSubClientId() {
        return this.subClientId;
    }

    @Override
    public ProxiedBedrockPeer getPeer() {
        return (ProxiedBedrockPeer) super.getPeer();
    }

    @Override
    public long getPing() {
        return this.getPeer().getPing();
    }

    @Override
    public PacketDirection getPacketDirection() {
        return PacketDirection.CLIENT_BOUND;
    }
}
