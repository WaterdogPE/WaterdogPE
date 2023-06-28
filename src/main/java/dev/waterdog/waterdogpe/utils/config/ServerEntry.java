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

package dev.waterdog.waterdogpe.utils.config;

import dev.waterdog.waterdogpe.network.serverinfo.ServerInfoType;
import lombok.ToString;
import org.cloudburstmc.protocol.common.util.Preconditions;

import java.net.InetSocketAddress;

/**
 * This is data class used for Configuration which holds basic information of the ServerInfo
 */
@ToString
public class ServerEntry {

    private final String serverName;
    private final InetSocketAddress address;
    private final InetSocketAddress publicAddress;
    private final String serverType;

    public ServerEntry(String serverName, InetSocketAddress address, InetSocketAddress publicAddress, String serverType) {
        Preconditions.checkArgument(serverName != null && !serverName.isEmpty(), "Server name is not valid");
        Preconditions.checkNotNull(address, "Server address can not be null");
        Preconditions.checkNotNull(serverType, "ServerInfoType can not be null");
        this.serverName = serverName;
        this.address = address;
        this.publicAddress = publicAddress;
        this.serverType = serverType;
    }

    public String getServerName() {
        return this.serverName;
    }

    public InetSocketAddress getAddress() {
        return this.address;
    }

    public InetSocketAddress getPublicAddress() {
        return this.publicAddress;
    }

    public String getServerType() {
        return this.serverType;
    }

    public ServerInfoType getServerInfoType() {
        ServerInfoType serverInfoType = ServerInfoType.fromString(this.serverType);
        if (serverInfoType == null) {
            throw new IllegalArgumentException("Unsupported ServerInfoType " + serverType + "! Make sure your config is valid and provided ServerInfoType was registered");
        }
        return serverInfoType;
    }
}
