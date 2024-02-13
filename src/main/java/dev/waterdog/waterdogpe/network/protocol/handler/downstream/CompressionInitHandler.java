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

package dev.waterdog.waterdogpe.network.protocol.handler.downstream;

import dev.waterdog.waterdogpe.network.connection.client.ClientConnection;
import dev.waterdog.waterdogpe.network.connection.codec.compression.CompressionType;
import dev.waterdog.waterdogpe.network.protocol.Signals;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import org.cloudburstmc.protocol.bedrock.netty.BedrockBatchWrapper;
import org.cloudburstmc.protocol.bedrock.packet.*;
import org.cloudburstmc.protocol.common.PacketSignal;

public class CompressionInitHandler extends AbstractDownstreamHandler {

    private final BedrockPacketHandler nextHandler;

    public CompressionInitHandler(ProxiedPlayer player, ClientConnection connection, BedrockPacketHandler nextHandler) {
        super(player, connection);
        this.nextHandler = nextHandler;

        RequestNetworkSettingsPacket packet = new RequestNetworkSettingsPacket();
        packet.setProtocolVersion(player.getProtocol().getProtocol());
        connection.sendPacket(packet);
    }

    @Override
    public void sendProxiedBatch(BedrockBatchWrapper batch) {
        // Noop. We are not forwarding anything yet
    }

    @Override
    public PacketSignal handle(NetworkSettingsPacket packet) {
        CompressionType compression = CompressionType.fromBedrockCompression(packet.getCompressionAlgorithm());
        this.connection.setCompression(compression);
        this.connection.setPacketHandler(nextHandler);
        this.connection.sendPacket(this.player.getLoginData().getLoginPacket());
        return Signals.CANCEL;
    }
}
