/*
 * Copyright 2026 WaterdogTEAM
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

package dev.waterdog.waterdogpe.network.nethernet;

import dev.waterdog.waterdogpe.network.connection.client.BedrockClientConnection;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import io.netty.channel.Channel;
import lombok.extern.log4j.Log4j2;

import javax.crypto.SecretKey;

/**
 * A downstream connection carried over NetherNet.
 */
@Log4j2
public class NetherNetClientConnection extends BedrockClientConnection {

    public NetherNetClientConnection(ProxiedPlayer player, ServerInfo serverInfo, Channel channel) {
        super(player, serverInfo, channel);
    }

    @Override
    public boolean supportsEncryption() {
        return false;
    }

    /**
     * NetherNet carries no Bedrock encryption, the data channel is already inside DTLS. A server
     * that offers a handshake anyway is misconfigured, and following it would desynchronize the
     * stream on the next packet.
     */
    @Override
    public void enableEncryption(SecretKey secretKey) {
        log.warn("{} offered a Bedrock encryption handshake over NetherNet, ignoring it", this.getServerInfo());
    }
}
