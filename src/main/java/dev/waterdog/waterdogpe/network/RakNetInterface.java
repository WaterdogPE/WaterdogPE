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

package dev.waterdog.waterdogpe.network;

import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.network.connection.codec.initializer.OfflineServerChannelInitializer;
import dev.waterdog.waterdogpe.network.connection.codec.initializer.ProxiedServerSessionInitializer;
import dev.waterdog.waterdogpe.network.serverinfo.BedrockServerInfo;
import dev.waterdog.waterdogpe.utils.types.TranslationContainer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.unix.UnixChannelOption;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.cloudburstmc.netty.channel.raknet.RakChannelFactory;
import org.cloudburstmc.netty.channel.raknet.config.RakChannelOption;
import org.cloudburstmc.netty.channel.raknet.config.RakServerCookieMode;

public class RakNetInterface implements NetworkInterface {

    private final List<Channel> serverChannels = new ObjectArrayList<>();

    private final ProxyServer server;

    private final long serverId;

    private boolean running = false;

    public RakNetInterface(ProxyServer server) {
        this.server = server;
        this.serverId = createRandomGUID();
    }

    @Override
    public void start(InetSocketAddress address) throws NetworkStartupException {
        List<Channel> bound = new ObjectArrayList<>();
        try {
            boolean allowEpoll = Epoll.isAvailable();
            int bindCount = allowEpoll && EventLoops.getChannelType() != EventLoops.ChannelType.NIO
                    ? Runtime.getRuntime().availableProcessors() : 1;

            for (int i = 0; i < bindCount; i++) {
                ServerBootstrap bootstrap = new ServerBootstrap()
                        .channelFactory(RakChannelFactory.server(EventLoops.getChannelType().getDatagramChannel()))
                        .group(this.server.getBossEventLoopGroup(), this.server.getWorkerEventLoopGroup())
                        // .option(CustomChannelOption.IP_DONT_FRAG, 2 /* IP_PMTUDISC_DO */)
                        .option(RakChannelOption.RAK_GUID, this.serverId)
                        .option(RakChannelOption.RAK_HANDLE_PING, true)
                        .option(RakChannelOption.RAK_MAX_MTU, this.server.getNetworkSettings().getMaximumMtu())
                        .option(RakChannelOption.RAK_SERVER_COOKIE_MODE, this.server.getNetworkSettings().enableCookies() ?
                                RakServerCookieMode.ACTIVE : RakServerCookieMode.INVALID)
                        .option(RakChannelOption.RAK_PROXY_PROTOCOL, this.server.getNetworkSettings().enableProxyProtocol())
                        .childOption(RakChannelOption.RAK_SESSION_TIMEOUT, 10000L)
                        .childOption(RakChannelOption.RAK_ORDERING_CHANNELS, 1)
                        .handler(new OfflineServerChannelInitializer(this.server))
                        .childHandler(new ProxiedServerSessionInitializer(this.server));
                if (allowEpoll) {
                    bootstrap.option(UnixChannelOption.SO_REUSEPORT, true);
                }
                ChannelFuture future = bootstrap
                        .bind(address)
                        .syncUninterruptibly();
                if (future.isSuccess()) {
                    bound.add(future.channel());
                } else {
                    throw new IllegalStateException("Can not start server on " + address, future.cause());
                }
            }
            this.serverChannels.addAll(bound);
            this.running = true;
            this.server.getLogger().info(new TranslationContainer("waterdog.query.start", address.toString()).getTranslated());
        } catch (Exception e) {
            for (Channel channel : bound) {
                if (channel.isOpen()) {
                    channel.close().syncUninterruptibly();
                }
            }
            throw new NetworkStartupException("Failed to start RakNet", e);
        }
    }

    @Override
    public void shutdown() {
        if (!running) {
            return;
        }
        running = false;
        for (Channel channel : this.serverChannels) {
            if (channel.isOpen()) {
                channel.close().syncUninterruptibly();
            }
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    // BDS accepts any GUID that is unique, but client itself sends negative GUID so we mirror that behavior.
    // Some raknet implementations like go-raknet seem to enforce this rule.
    public static long createRandomGUID() {
        return ThreadLocalRandom.current().nextLong(Long.MIN_VALUE, 0);
    }
}
