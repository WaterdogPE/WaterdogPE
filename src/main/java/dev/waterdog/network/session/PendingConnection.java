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

package dev.waterdog.network.session;

import com.nukkitx.protocol.bedrock.BedrockClient;
import dev.waterdog.network.ServerInfo;

public class PendingConnection {

    private final ServerInfo serverInfo;
    private BedrockClient client;

    public PendingConnection(ServerInfo serverInfo) {
        this.serverInfo = serverInfo;
    }

    public void close() {
        if (this.client != null) {
            this.client.close();
        }
    }

    public ServerInfo getInfo() {
        return this.serverInfo;
    }

    public BedrockClient getClient() {
        return this.client;
    }

    public void setClient(BedrockClient client) {
        this.client = client;
    }
}
