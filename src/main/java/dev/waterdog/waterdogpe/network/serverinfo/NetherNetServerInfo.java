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

package dev.waterdog.waterdogpe.network.serverinfo;

import org.cloudburstmc.netty.channel.nethernet.NetherNetChannelFactory;
import org.cloudburstmc.netty.channel.nethernet.config.NetherChannelOption;
import org.cloudburstmc.netty.channel.nethernet.signaling.NetherNetHTTPClientSignaling;
import org.cloudburstmc.netty.util.nethernet.OperatorIdentity;
import dev.waterdog.waterdogpe.network.connection.client.ClientConnection;
import dev.waterdog.waterdogpe.network.connection.codec.initializer.NetherNetClientSessionInitializer;
import dev.waterdog.waterdogpe.network.nethernet.ProxyIdentity;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

import java.net.InetSocketAddress;

/**
 * A downstream server reached over NetherNet.
 */
public class NetherNetServerInfo extends ServerInfo {

    /** One attempt: the fallback lives in {@link AutoServerInfo}, retrying here would only delay it. */
    private static final int HANDSHAKE_ATTEMPTS = 1;

    public NetherNetServerInfo(String serverName, InetSocketAddress address, InetSocketAddress publicAddress) {
        super(serverName, address, publicAddress);
    }

    @Override
    public ServerInfoType getServerType() {
        return ServerInfoType.NETHERNET;
    }

    @Override
    public Future<ClientConnection> createConnection(ProxiedPlayer player) {
        return connect(player, this);
    }

    static Future<ClientConnection> connect(ProxiedPlayer player, ServerInfo serverInfo) {
        EventLoop eventLoop = player.getProxy().getWorkerEventLoopGroup().next();
        Promise<ClientConnection> promise = eventLoop.newPromise();
        InetSocketAddress remoteAddress = serverInfo.getResolvedAddress();

        int timeout = player.getProxy().getNetworkSettings().getConnectTimeout() * 1000;

        // Derived per connection: the token names the player and expires
        OperatorIdentity identity;
        try {
            identity = ProxyIdentity.identity(player.getProxy()).forPlayer(player.getXuid(), player.getName());
        } catch (Exception e) {
            promise.tryFailure(e);
            return promise;
        }

        new Bootstrap()
                .group(eventLoop)
                .channelFactory(NetherNetChannelFactory.client(NetherNetHTTPClientSignaling::new))
                .option(NetherChannelOption.NETHER_CLIENT_HANDSHAKE_TIMEOUT_MS, timeout)
                .option(NetherChannelOption.NETHER_CLIENT_MAX_HANDSHAKE_ATTEMPTS, HANDSHAKE_ATTEMPTS)
                .option(NetherChannelOption.NETHER_CLIENT_IDENTITY, identity)
                .handler(new NetherNetClientSessionInitializer(player, serverInfo, promise))
                .connect(remoteAddress)
                .addListener((ChannelFuture future) -> {
                    if (!future.isSuccess()) {
                        promise.tryFailure(future.cause());
                        future.channel().close();
                    }
                });
        return promise;
    }
}
