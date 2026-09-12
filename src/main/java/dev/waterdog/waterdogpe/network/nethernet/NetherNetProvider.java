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

import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.WaterdogPE;
import dev.waterdog.waterdogpe.network.connection.codec.initializer.NetherNetServerSessionInitializer;
import dev.waterdog.waterdogpe.network.protocol.ProtocolVersion;
import dev.waterdog.waterdogpe.utils.config.proxy.NetherNetSettings;
import dev.waterdog.waterdogpe.utils.config.proxy.NxsSettings;
import dev.waterdog.waterdogpe.utils.config.proxy.ProxyConfig;
import dev.waterdog.waterdogpe.utils.ThreadFactoryBuilder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.netty.signalling.ProviderClient;
import org.cloudburstmc.netty.signalling.ProviderStateStore;
import org.cloudburstmc.netty.signalling.ProviderTransport;
import org.cloudburstmc.netty.signalling.ServerStatus;
import org.cloudburstmc.netty.signalling.provider.NativeProviderHostFactory;
import org.cloudburstmc.netty.signalling.provider.ProviderHostFactory;
import org.cloudburstmc.netty.signalling.provider.ProviderRuntimeConfiguration;
import org.cloudburstmc.netty.signalling.provider.ProviderRuntimeObservations;
import org.cloudburstmc.netty.signalling.provider.ProviderShutdown;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Registers the proxy with an NXS provider, which then hands it players.
 * <p>
 * Nothing is served here. The provider admits a peer and the proxy receives it on a UDP port of its
 * own, so this is the other half of {@link NetherNetInterface}'s builtin endpoint rather than a
 * variation on it, and the two can run together.
 */
@Log4j2
public class NetherNetProvider implements AutoCloseable {

    private static final String LABEL = "WaterdogPE";
    private static final Set<String> LOCAL_ORIGINS = Set.of("127.0.0.1", "localhost", "[::1]");

    private final ProxyServer proxy;

    private EventLoopGroup group;
    private Channel channel;
    private ProviderClient client;
    private ProviderShutdown shutdown;

    public NetherNetProvider(ProxyServer proxy) {
        this.proxy = proxy;
    }

    /**
     * @param settings The NetherNet settings, for the provider section and the UDP port
     * @param address  The address the proxy listens on
     * @param udpPort  The fixed UDP port peers are handed, which the provider cannot advertise if
     *                 it is ephemeral
     */
    public void start(NetherNetSettings settings, InetSocketAddress address, int udpPort) throws Exception {
        NxsSettings nxs = settings.getNxs();
        Path dataPath = this.proxy.getDataPath();

        ProviderRuntimeConfiguration runtime = ProviderRuntimeConfiguration.resolve(
                new ProviderRuntimeConfiguration.Settings(nxs.getEndpoint(), nxs.getToken(),
                        nxs.getAdvertiseAddresses(), nxs.getData()),
                dataPath, address.getAddress().getHostAddress(), udpPort,
                this.proxy.getConfiguration().getMaxPlayerCount(), LABEL);

        ProviderStateStore store = new ProviderStateStore(runtime.stateDirectory());
        ProviderTransport transport = null;
        try {
            this.group = new NioEventLoopGroup(1, ThreadFactoryBuilder.builder()
                    .format("NetherNet Provider - #%d").build());
            ServerBootstrap bootstrap = new ServerBootstrap()
                    .group(this.group)
                    .childHandler(new NetherNetServerSessionInitializer(this.proxy));

            ProviderHostFactory.Host host = new NativeProviderHostFactory()
                    .open(bootstrap, new InetSocketAddress(runtime.bindAddress(), runtime.udpPort()),
                            Map.of("stateDirectory", runtime.stateDirectory().toAbsolutePath().toString(),
                                    "profile", runtime.profile(),
                                    "advertisedEndpoints", runtime.encodedAdvertisedEndpoints(),
                                    "localDevelopment",
                                    Boolean.toString(LOCAL_ORIGINS.contains(runtime.origin().getHost()))))
                    .toCompletableFuture().get(30, TimeUnit.SECONDS);

            this.channel = host.channel();
            transport = host.transport();
            host.warnings().forEach(log::warn);

            this.client = new ProviderClient(runtime.clientConfiguration(), store, transport, this::status,
                    () -> ProviderRuntimeObservations.health(this.players(), runtime.capacity(),
                            System.currentTimeMillis(), WaterdogPE.version().baseVersion(), this.accepting()),
                    log::warn);
            // The client owns the store and the transport from here
            store = null;
            transport = null;
            this.shutdown = new ProviderShutdown(this.client::stop, this::closeNetwork, log::warn);
        } finally {
            if (transport != null) {
                transport.close();
            }
            if (store != null) {
                store.close();
            }
        }

        this.client.start().whenComplete((registration, failure) -> {
            if (failure != null) {
                log.error("NetherNet provider registration failed, no players will arrive through it", failure);
                this.close();
                return;
            }
            log.info(ProviderRuntimeObservations.registrationMessage(registration));
        });
    }

    @Override
    public void close() {
        ProviderShutdown shutdown = this.shutdown;
        this.shutdown = null;
        if (shutdown != null) {
            // Closes the client, which drains at the provider before the endpoint goes away
            shutdown.close();
            return;
        }
        this.closeNetwork();
    }

    private void closeNetwork() {
        if (this.channel != null) {
            this.channel.close();
            this.channel = null;
        }
        if (this.group != null) {
            this.group.shutdownGracefully();
            this.group = null;
        }
        this.client = null;
    }

    /**
     * What the provider shows for this proxy, which is the same thing a player sees in the list.
     */
    private ServerStatus status() {
        ProxyConfig config = this.proxy.getConfiguration();
        return new ServerStatus(config.getMotd(), ProtocolVersion.latest().getProtocol(),
                ProtocolVersion.latest().getMinecraftVersion(), config.getSubMotd(), this.players(),
                config.getMaxPlayerCount(), 0);
    }

    private int players() {
        return this.proxy.getPlayerManager().getPlayers().size();
    }

    /**
     * Whether the proxy wants more players, which is separate from whether it is healthy: a full
     * proxy is working perfectly and simply has nowhere to put them.
     */
    private boolean accepting() {
        return this.players() < this.proxy.getConfiguration().getMaxPlayerCount();
    }
}
