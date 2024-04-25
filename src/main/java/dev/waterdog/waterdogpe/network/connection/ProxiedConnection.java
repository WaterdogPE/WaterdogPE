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

package dev.waterdog.waterdogpe.network.connection;

import org.cloudburstmc.protocol.bedrock.PacketDirection;
import org.cloudburstmc.protocol.bedrock.netty.BedrockBatchWrapper;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacketHandler;

import java.net.SocketAddress;

public interface ProxiedConnection {

    void sendPacket(BedrockBatchWrapper wrapper);

    void sendPacket(BedrockPacket packet);

    default void sendPacketImmediately(BedrockPacket packet) {
        this.sendPacket(packet);
    }

    BedrockPacketHandler getPacketHandler();

    void setPacketHandler(BedrockPacketHandler handler);

    SocketAddress getSocketAddress();

    boolean isConnected();

    default int getSubClientId() {
        return 0;
    }

    long getPing();

    PacketDirection getPacketDirection();
}
