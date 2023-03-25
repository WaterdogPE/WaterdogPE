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

import dev.waterdog.waterdogpe.network.connection.codec.BedrockBatchWrapper;
import dev.waterdog.waterdogpe.network.connection.codec.server.PacketQueueHandler;
import io.netty.channel.ChannelPipeline;
import org.cloudburstmc.protocol.bedrock.BedrockDisconnectReasons;
import org.cloudburstmc.protocol.bedrock.BedrockPeer;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.DisconnectPacket;

public class BedrockServerSession extends ProxiedBedrockSession {

    public BedrockServerSession(ProxiedBedrockPeer peer, int subClientId) {
        super(peer, subClientId);
    }

    @Override
    public void disconnect(String reason, boolean hideReason) {
        this.checkForClosed();

        DisconnectPacket packet = new DisconnectPacket();
        if (reason == null || hideReason) {
            packet.setMessageSkipped(true);
            reason = BedrockDisconnectReasons.DISCONNECTED;
        }
        packet.setKickMessage(reason);
        this.sendPacketImmediately(packet);
    }

    @Override
    public void sendPacketImmediately(BedrockPacket packet) {
        this.getPeer().sendPacket(BedrockBatchWrapper.create(this.subClientId, packet).skipQueue(true));
    }

    public void setTransferQueueActive(boolean enable) {
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
}
