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

package dev.waterdog.waterdogpe.network.session.bedrock;

import com.google.common.base.Preconditions;
import com.nukkitx.protocol.bedrock.BedrockClient;
import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.network.protocol.ProtocolVersion;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo;
import dev.waterdog.waterdogpe.network.session.DownstreamClient;
import dev.waterdog.waterdogpe.network.session.DownstreamSession;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class BedrockDefaultClient implements DownstreamClient {

    private final ServerInfo serverInfo;
    private BedrockClient client;
    private BedrockDefaultSession session;

    public BedrockDefaultClient(ServerInfo serverInfo) {
        this.serverInfo = serverInfo;
    }

    @Override
    public CompletableFuture<DownstreamClient> bindDownstream(ProtocolVersion protocol) {
        this.client = ProxyServer.getInstance().createBedrockClient();
        this.client.setRakNetVersion(protocol.getRaknetVersion());
        return this.client.bind().thenApply(i -> this);
    }

    @Override
    public CompletableFuture<DownstreamSession> connect(InetSocketAddress address, long timeout, TimeUnit unit) {
        Preconditions.checkNotNull(this.client, "Client was not initialized!");
        return this.client.connect(address, timeout, unit).thenApply(downstream -> this.session = new BedrockDefaultSession(this, downstream));
    }

    @Override
    public InetSocketAddress getBindAddress() {
        Preconditions.checkNotNull(this.client, "Client was not initialized!");
        return this.client.getBindAddress();
    }

    @Override
    public void close(boolean force) {
        if (!force && this.session != null && !this.session.isClosed()) {
            this.session.disconnect();
        } else if (this.client != null){
            this.client.close(force);
        }
    }

    @Override
    public boolean isConnected() {
        return this.session != null && !this.session.isClosed();
    }

    @Override
    public ServerInfo getServerInfo() {
        return this.serverInfo;
    }

    @Override
    public BedrockDefaultSession getSession() {
        return this.session;
    }
}
