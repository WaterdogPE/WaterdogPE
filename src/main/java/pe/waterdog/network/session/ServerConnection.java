/**
 * Copyright 2020 WaterdogTEAM
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pe.waterdog.network.session;

import com.nukkitx.protocol.bedrock.BedrockClient;
import com.nukkitx.protocol.bedrock.BedrockClientSession;
import com.nukkitx.protocol.bedrock.BedrockPacket;
import pe.waterdog.network.ServerInfo;

import java.net.InetSocketAddress;

public class ServerConnection {

    private final ServerInfo serverInfo;

    private final BedrockClient client;
    private final BedrockClientSession downstream;

    public ServerConnection(BedrockClient client, BedrockClientSession session, ServerInfo serverInfo){
        this.client = client;
        this.downstream = session;
        this.serverInfo = serverInfo;
    }

    public void sendPacket(BedrockPacket packet){
        if (!this.downstream.isClosed()){
            this.downstream.sendPacket(packet);
        }
    }

    public void disconnect(){
        if (!this.downstream.isClosed()){
            this.downstream.disconnect();
        }
        this.client.close();
    }

    public ServerInfo getInfo() {
        return this.serverInfo;
    }

    public BedrockClientSession getDownstream() {
        return this.downstream;
    }

    public InetSocketAddress getAddress(){
        return this.downstream.getAddress();
    }

    public boolean isConnected() {
        return !this.downstream.isClosed();
    }
}
