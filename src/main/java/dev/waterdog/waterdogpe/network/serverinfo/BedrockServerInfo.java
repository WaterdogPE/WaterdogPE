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
import dev.waterdog.waterdogpe.network.RakNetInterface;
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
import org.cloudburstmc.netty.channel.raknet.RakPing;
import org.cloudburstmc.netty.channel.raknet.config.RakChannelOption;
import org.cloudburstmc.protocol.bedrock.BedrockPong;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class BedrockServerInfo extends ServerInfo {

    private static volatile Boolean loopbackBindSupported;

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
        InetSocketAddress remoteAddress = this.getResolvedAddress();
        Bootstrap bootstrap = new Bootstrap()
                .channelFactory(RakChannelFactory.client(EventLoops.getChannelType().getDatagramChannel()))
                .group(eventLoop)
                .option(RakChannelOption.RAK_GUID, RakNetInterface.createRandomGUID())
                .option(RakChannelOption.RAK_PROTOCOL_VERSION, version.getRaknetVersion())
                .option(RakChannelOption.RAK_ORDERING_CHANNELS, 1)
                .option(RakChannelOption.RAK_CONNECT_TIMEOUT, networkSettings.getConnectTimeout() * 1000L)
                .option(RakChannelOption.RAK_SESSION_TIMEOUT, 10000L)
                .option(RakChannelOption.RAK_MTU, networkSettings.getMaximumDownstreamMtu())
                .handler(new ProxiedClientSessionInitializer(player, this, promise));
        if (networkSettings.randomDownstreamLoopbackAddress()
                && remoteAddress.getAddress() instanceof Inet4Address
                && remoteAddress.getAddress().isLoopbackAddress()
                && isLoopbackBindSupported()) {
            bootstrap.localAddress(randomLoopbackAddress());
        }
        bootstrap.connect(remoteAddress).addListener((ChannelFuture future) -> {
            if (!future.isSuccess()) {
                promise.tryFailure(future.cause());
                future.channel().close();
            }
        });
        return promise;
    }

    /**
     * Sidesteps RakNet's per-IP connection rate limit by giving each connection its own source
     * address from the loopback range.
     */
    private static InetSocketAddress randomLoopbackAddress() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        byte[] address = {127, (byte) random.nextInt(1, 255), (byte) random.nextInt(1, 255), (byte) random.nextInt(1, 255)};
        try {
            return new InetSocketAddress(InetAddress.getByAddress(address), 0);
        } catch (UnknownHostException e) {
            throw new AssertionError(e); // 4-byte address is always valid
        }
    }

    // Linux and Windows allow binding the whole 127.0.0.0/8 out of the box, macOS does not
    private static boolean isLoopbackBindSupported() {
        Boolean supported = loopbackBindSupported;
        if (supported == null) {
            try (DatagramSocket socket = new DatagramSocket(new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 1, 2, 3}), 0))) {
                supported = true;
            } catch (IOException e) {
                ProxyServer.getInstance().getLogger().warning("This host cannot bind random loopback addresses, using the default source address", e);
                supported = false;
            }
            loopbackBindSupported = supported;
        }
        return supported;
    }

    public Future<BedrockPong> ping(long timeout, TimeUnit timeUnit) {
        EventLoop eventLoop = ProxyServer.getInstance().getWorkerEventLoopGroup().next();
        Promise<BedrockPong> promise = eventLoop.newPromise();

        new Bootstrap()
                .channelFactory(RakChannelFactory.client(EventLoops.getChannelType().getDatagramChannel()))
                .group(eventLoop)
                .option(RakChannelOption.RAK_GUID, RakNetInterface.createRandomGUID())
                .handler(new ClientPingHandler(promise, timeout, timeUnit))
                .bind(0)
                .addListener((ChannelFuture future) -> {
                    if (future.cause() != null) {
                        promise.tryFailure(future.cause());
                        future.channel().close();
                    } else {
                        RakPing ping = new RakPing(System.currentTimeMillis(), this.getResolvedAddress());
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
