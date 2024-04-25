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

package dev.waterdog.waterdogpe.network.connection.client;

import dev.waterdog.waterdogpe.network.connection.ProxiedConnection;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import org.cloudburstmc.protocol.bedrock.PacketDirection;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodecHelper;
import org.cloudburstmc.protocol.bedrock.data.CompressionAlgorithm;
import org.cloudburstmc.protocol.bedrock.netty.codec.compression.CompressionStrategy;

import javax.crypto.SecretKey;

public interface ClientConnection extends ProxiedConnection {
    String NAME = "client-connection";

    ServerInfo getServerInfo();

    ProxiedPlayer getPlayer();

    void setCodecHelper(BedrockCodec codec, BedrockCodecHelper helper);

    default void enableEncryption(SecretKey secretKey) {
        // No encryption by default
    }

    void setCompression(CompressionAlgorithm compression);

    void setCompressionStrategy(CompressionStrategy strategy);

    void addDisconnectListener(Runnable listener);

    void disconnect();

    @Override
    default PacketDirection getPacketDirection() {
        return PacketDirection.SERVER_BOUND;
    }
}
