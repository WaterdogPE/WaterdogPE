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

package dev.waterdog.waterdogpe.network.serverinfo;

import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.network.EventLoops;
import dev.waterdog.waterdogpe.network.connection.client.ClientConnection;
import dev.waterdog.waterdogpe.network.connection.codec.client.ClientPingHandler;
import dev.waterdog.waterdogpe.network.connection.codec.initializer.ProxiedClientSessionInitializer;
import dev.waterdog.waterdogpe.network.protocol.ProtocolVersion;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import dev.waterdog.waterdogpe.utils.config.proxy.NetworkSettings;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import org.cloudburstmc.netty.channel.raknet.RakChannelFactory;
import org.cloudburstmc.netty.channel.raknet.RakConstants;
import org.cloudburstmc.netty.channel.raknet.RakPing;
import org.cloudburstmc.netty.channel.raknet.config.RakChannelOption;
import org.cloudburstmc.protocol.bedrock.BedrockPong;

import java.net.InetSocketAddress;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class BedrockServerInfo extends ServerInfo {

    public BedrockServerInfo(String serverName, InetSocketAddress address, InetSocketAddress publicAddress) {
        super(serverName, address, publicAddress);
    }

    @Override
    public ServerInfoType getServerType() {
        return ServerInfoType.BEDROCK;
    }

    @Override
    public Future<ClientConnection> createConnection(ProxiedPlayer player) {
        ProtocolVersion version = player.getProtocol();
        NetworkSettings networkSettings = player.getProxy().getNetworkSettings();

        // Just pick EventLoop here, and we can use it for our promise too
        EventLoop eventLoop = player.getProxy().getWorkerEventLoopGroup().next();
        Promise<ClientConnection> promise = eventLoop.newPromise();
        new Bootstrap()
                .channelFactory(RakChannelFactory.client(EventLoops.getChannelType().getDatagramChannel()))
                .group(eventLoop)
                .option(RakChannelOption.RAK_PROTOCOL_VERSION, version.getRaknetVersion())
                .option(RakChannelOption.RAK_ORDERING_CHANNELS, 1)
                .option(RakChannelOption.RAK_CONNECT_TIMEOUT, networkSettings.getConnectTimeout() * 1000L)
                .option(RakChannelOption.RAK_SESSION_TIMEOUT, 10000L)
                .option(RakChannelOption.RAK_MTU, Integer.MAX_VALUE)
                .option(RakChannelOption.RAK_MAX_QUEUED_BYTES, 0)
                .option(RakChannelOption.RAK_GLOBAL_PACKET_LIMIT, Integer.MAX_VALUE)
                .option(RakChannelOption.RAK_PACKET_LIMIT, Integer.MAX_VALUE)
                .handler(new ProxiedClientSessionInitializer(player, this, promise))
                .connect(this.getAddress()).addListener((ChannelFuture future) -> {
                    if (!future.isSuccess()) {
                        promise.tryFailure(future.cause());
                        future.channel().close();
                    }
                });
        return promise;
    }

    public Future<BedrockPong> ping(long timeout, TimeUnit timeUnit) {
        EventLoop eventLoop = ProxyServer.getInstance().getWorkerEventLoopGroup().next();
        Promise<BedrockPong> promise = eventLoop.newPromise();

        new Bootstrap()
                .channelFactory(RakChannelFactory.client(EventLoops.getChannelType().getDatagramChannel()))
                .group(eventLoop)
                .option(RakChannelOption.RAK_GUID, ThreadLocalRandom.current().nextLong())
                .handler(new ClientPingHandler(promise, timeout, timeUnit))
                .bind(ThreadLocalRandom.current().nextInt(10000, 15000))
                .addListener((ChannelFuture future) -> {
                    if (future.cause() != null) {
                        promise.tryFailure(future.cause());
                        future.channel().close();
                    } else {
                        RakPing ping = new RakPing(System.currentTimeMillis(), this.getAddress());
                        future.channel().writeAndFlush(ping).addListener(future1 -> {
                            if (future1.cause() != null) {
                                promise.tryFailure(future1.cause());
                                future.channel().close();
                            }
                        });
                    }
                });
        return promise;
    }
}
