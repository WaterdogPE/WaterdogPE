/*
 * Copyright 2021 WaterdogTEAM
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

package dev.waterdog.waterdogpe.network.serverinfo;

import dev.waterdog.waterdogpe.network.protocol.ProtocolVersion;
import dev.waterdog.waterdogpe.network.session.DownstreamClient;
import dev.waterdog.waterdogpe.network.session.bedrock.BedrockDefaultClient;

import java.net.InetSocketAddress;

/**
 * This is the default Minecraft: Bedrock ServerInfo implementation which
 * uses UDP RakNet protocol thanks to CloudBurst protocol library.
 */
public class BedrockServerInfo extends ServerInfo {

    public BedrockServerInfo(String serverName, InetSocketAddress address, InetSocketAddress publicAddress) {
        super(serverName, address, publicAddress);
    }

    @Override
    public DownstreamClient createNewConnection(ProtocolVersion protocol) {
        return new BedrockDefaultClient(this);
    }

    @Override
    public ServerInfoType getServerType() {
        return ServerInfoType.BEDROCK;
    }
}
