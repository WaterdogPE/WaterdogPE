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
import dev.waterdog.waterdogpe.utils.config.proxy.NetherNetSettings;
import dev.waterdog.waterdogpe.utils.config.proxy.NxsAdvancedSettings;
import dev.waterdog.waterdogpe.utils.config.proxy.NxsSettings;
import dev.waterdog.waterdogpe.utils.config.proxy.ProxyConfig;
import dev.waterdog.waterdogpe.utils.ThreadFactoryBuilder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.netty.signaling.ProviderClient;
import org.cloudburstmc.netty.signaling.ProviderDiagnostic;
import org.cloudburstmc.netty.signaling.ProviderStateStore;
import org.cloudburstmc.netty.signaling.ProviderTransport;
import org.cloudburstmc.netty.signaling.ServerStatus;
import org.cloudburstmc.netty.signaling.provider.NativeProviderHostFactory;
import org.cloudburstmc.netty.signaling.provider.ProviderRuntimeConfiguration;
import org.cloudburstmc.netty.signaling.provider.ProviderShutdown;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Registers the proxy with an NXS provider, which then hands it players.
 * <p>
 * Nothing is served here. The provider admits a peer and the proxy receives it on a UDP port of its
 * own, so this is the other half of {@link NetherNetInterface}'s builtin endpoint rather than a
 * variation on it, and the two can run together.
 */
@Log4j2
public class NetherNetProvider implements AutoCloseable {

    private final ProxyServer proxy;

    private EventLoopGroup group;
    private volatile Channel channel;
    private volatile ProviderClient client;
    private final GameOutcomeReporter gameOutcomes = new GameOutcomeReporter(this::reportGameOutcome);
    private ProviderShutdown shutdown;
    /** What the provider is told about this proxy. Swapped out by a status override. */
    private volatile Supplier<ServerStatus> statusSupplier = this::collectStatus;

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

        ProviderRuntimeConfiguration runtime = ProviderRuntimeConfiguration.resolve(providerSettings(nxs), dataPath, address.getAddress().getHostAddress(), udpPort,
                this.proxy.getConfiguration().getMaxPlayerCount());

        ProviderStateStore store = new ProviderStateStore(runtime.stateDirectory());
        ProviderTransport transport = null;
        try {
            this.group = new NioEventLoopGroup(1, ThreadFactoryBuilder.builder()
                    .format("NetherNet Provider - #%d").build());
            ServerBootstrap bootstrap = new ServerBootstrap()
                    .group(this.group)
                    .childHandler(new NetherNetServerSessionInitializer(this.proxy, this.gameOutcomes));

            NativeProviderHostFactory.Host host = NativeProviderHostFactory.open(bootstrap, runtime)
                    .toCompletableFuture().get(30, TimeUnit.SECONDS);

            this.channel = host.channel();
            transport = host.transport();
            host.warnings().forEach(log::warn);

            this.client = new ProviderClient(runtime.clientConfiguration(), store, transport,
                    () -> this.statusSupplier.get(),
                    () -> this.health(runtime.capacity()),
                    NetherNetProvider::log);
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

        ProviderClient client = this.client;
        client.start().whenComplete((registration, failure) -> {
            if (failure != null) {
                log.error("NetherNet provider registration failed, no players will arrive through it", failure);
                this.close();
                return;
            }
            log.info(registrationMessage(registration));
            // The provider may offer the operator a link, such as one to claim this proxy
            client.operatorAction().thenAccept(action -> action.ifPresent(value -> log.info(operatorActionMessage(value))));
            // Says where logins are vouched for, without putting provider credentials in the log
            log.debug("NetherNet client identities are authenticated by external signaling provider "
                    + runtime.origin().getHost() + "; direct login keys are checked against its admission tickets");
        });
    }

    /**
     * What the provider runtime is configured with, named like Geyser's options. The proxy reports
     * game outcomes itself.
     */
    static ProviderRuntimeConfiguration.Settings providerSettings(NxsSettings nxs) throws IOException {
        NxsAdvancedSettings advanced = nxs.getAdvanced();
        ProviderClient.Location location;
        try {
            location = ProviderClient.Location.of(advanced.getLocation());
        } catch (IllegalArgumentException invalid) {
            throw new IOException("nethernet.nxs.advanced.location needs a two-letter country code, a latitude and longitude pair, or both; city is optional");
        }
        return ProviderRuntimeConfiguration.Settings.builder(nxs.getEndpoint())
                .token(nxs.getToken())
                .advertiseAddresses(nxs.getAdvertiseAddresses())
                .assistedJoins(advanced.isAssistedJoins())
                .connectionChecks(advanced.isConnectionChecks())
                .stunDiscovery(advanced.isStunDiscovery())
                .deregisterOnShutdown(advanced.isDeregisterOnShutdown())
                .location(location)
                .gameOutcomes(true)
                .build();
    }

    public static String operatorActionMessage(ProviderClient.OperatorAction action) {
        return action.text() + " " + action.url();
    }

    private ProviderClient.Health health(int capacity) {
        return new ProviderClient.Health(this.accepting(), capacity,
                new ProviderClient.Software("WaterdogPE", WaterdogPE.version().baseVersion()),
                new ProviderClient.ConnectedPlayers(this.players(), System.currentTimeMillis()));
    }

    private static void log(ProviderDiagnostic diagnostic) {
        String message = "NXS: " + diagnostic.message();
        switch (diagnostic.level()) {
            case DEBUG -> log.debug(message);
            case INFO -> log.info(message);
            case WARN -> log.warn(message);
        }
    }

    private static String registrationMessage(ProviderClient.Registration registration) {
        String instanceId = registration.instanceId();
        String address = registration.publicAddress();
        return address != null
                ? "Provider address: " + address + " (instance " + instanceId + ")"
                : "Provider instance registered: " + instanceId
                        + "; public addresses are managed on its attached Signal Servers";
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
     * Whether the provider registration is up. False before {@link #start} and after a failure.
     */
    public boolean isRunning() {
        return this.client != null;
    }

    private boolean reportGameOutcome(String ticketId, ProviderClient.GameOutcome outcome, String reason) {
        ProviderClient client = this.client;
        return client != null && client.reportGameOutcome(ticketId, outcome, reason);
    }

    public GameOutcomeReporter gameOutcomes() {
        return this.gameOutcomes;
    }

    public ProviderClient client() {
        return this.client;
    }

    /**
     * The endpoint the provider admits peers onto, for diagnostics.
     */
    public Channel channel() {
        return this.channel;
    }

    /**
     * The status the provider currently gets, whether collected or overridden.
     */
    public ServerStatus serverStatus() {
        return this.statusSupplier.get();
    }

    /**
     * Pins the provider status to a fixed snapshot. Values fixed at the provider still win.
     */
    public void setServerStatus(ServerStatus snapshot) {
        this.statusSupplier = () -> snapshot;
        this.refreshStatus();
    }

    public void restoreAutomaticServerStatus() {
        this.statusSupplier = this::collectStatus;
        this.refreshStatus();
    }

    /**
     * Pushes the current status on the next heartbeat. Unchanged snapshots send nothing.
     */
    public void refreshStatus() {
        ProviderClient client = this.client;
        if (client != null) {
            client.requestStatusRefresh();
        }
    }

    /**
     * The provider's own error text where it gave one, otherwise only the exception type, so
     * nothing internal reaches a command output.
     */
    public static String failureMessage(Throwable failure) {
        while (failure.getCause() != null
                && (failure instanceof CompletionException || failure instanceof ExecutionException)) {
            failure = failure.getCause();
        }
        return failure instanceof ProviderClient.ProviderException ? failure.getMessage()
                : failure.getClass().getSimpleName();
    }

    /**
     * What the provider shows for this proxy, which is the same thing a player sees in the list.
     */
    private ServerStatus collectStatus() {
        ProxyConfig config = this.proxy.getConfiguration();
        return new ServerStatus(config.getMotd(), config.getSubMotd(), config.getMaxPlayerCount(), 0);
    }

    private int players() {
        return this.proxy.getPlayerManager().getPlayers().size();
    }

    /**
     * Whether the proxy has room for more players.
     */
    private boolean accepting() {
        return this.players() < this.proxy.getConfiguration().getMaxPlayerCount();
    }
}
