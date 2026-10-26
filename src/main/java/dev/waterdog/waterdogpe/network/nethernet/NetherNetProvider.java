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
import dev.waterdog.waterdogpe.event.defaults.ProxyPingEvent;
import dev.waterdog.waterdogpe.network.connection.codec.initializer.NetherNetServerSessionInitializer;
import dev.waterdog.waterdogpe.network.protocol.ProtocolVersion;
import dev.waterdog.waterdogpe.utils.config.proxy.NxsAdvancedSettings;
import dev.waterdog.waterdogpe.utils.config.proxy.NxsSettings;
import dev.waterdog.waterdogpe.utils.config.proxy.SignalingSettings;
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
import org.cloudburstmc.netty.util.nethernet.IpRangeSet;
import org.cloudburstmc.netty.util.nethernet.SecretValue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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

    /** NetherNet's own files, next to config.yml, as Geyser keeps them. */
    static final String DATA_FOLDER = "nethernet";
    /**
     * Holds the provider's link for the operator, in {@link #DATA_FOLDER}.
     */
    private static final String OPERATOR_LINK_FILE = "operator-link.txt";
    /** Where network keeps the registration, inside the folder it is handed. */
    private static final String STATE_DIRECTORY = "provider-state";

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
     * @param settings The signaling settings, for the provider section and the addresses
     * @param address  The address the proxy listens on
     * @param udpPort  The UDP port peers are handed
     * @param relays   The PROXY protocol relays in front of that port
     */
    public void start(SignalingSettings settings, InetSocketAddress address, int udpPort, IpRangeSet relays) throws Exception {
        Path dataPath = this.proxy.getDataPath();
        Path folder = dataPath.resolve(DATA_FOLDER);
        moveLegacyState(dataPath);

        ProviderRuntimeConfiguration runtime = ProviderRuntimeConfiguration.resolve(providerSettings(settings, dataPath, relays), folder,
                address.getAddress().getHostAddress(), udpPort, bounded(this.proxy.callPingEvent(null).getMaximumPlayerCount()));

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
                    this::health,
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
            client.operatorAction().thenAccept(this::announceOperatorAction);
            // Says where logins are vouched for, without putting provider credentials in the log
            log.debug("NetherNet client identities are authenticated by external signaling provider "
                    + runtime.origin().getHost() + "; direct login keys are checked against its admission tickets");
        });
    }

    /**
     * Moves the registration kept next to config.yml before the nethernet folder existed, so the
     * provider knows this proxy again rather than registering it as a new one.
     */
    static void moveLegacyState(Path dataPath) throws IOException {
        Path legacy = dataPath.resolve(STATE_DIRECTORY);
        Path state = dataPath.resolve(DATA_FOLDER).resolve(STATE_DIRECTORY);
        if (Files.exists(legacy) && !Files.exists(state)) {
            try {
                Files.createDirectories(state.getParent());
                Files.move(legacy, state);
            } catch (IOException e) {
                // Starting without it would register this proxy as a new one
                throw new IOException("Could not move the NXS registration from " + legacy + " to " + state
                        + ", move it there by hand: " + e.getMessage(), e);
            }
            log.info("Moved the NXS registration from {} to {}", legacy, state);
        }
    }

    /**
     * What the provider runtime is configured with, named like Geyser's options. The proxy reports
     * game outcomes itself.
     *
     * @param dataPath The proxy's folder, which a file: token is read relative to like every other
     *                 secret, where network would read it relative to the nethernet folder
     * @param relays   The PROXY protocol relays in front of the media port
     */
    static ProviderRuntimeConfiguration.Settings providerSettings(SignalingSettings settings, Path dataPath,
                                                                  IpRangeSet relays) throws IOException {
        NxsSettings nxs = settings.getNxs();
        NxsAdvancedSettings advanced = nxs.getAdvanced();
        ProviderClient.Location location;
        try {
            location = ProviderClient.Location.of(advanced.getLocation());
        } catch (IllegalArgumentException invalid) {
            throw new IOException("nethernet.signaling.nxs.advanced.location needs a two-letter country code, a latitude and longitude pair, or both; city is optional");
        }
        String token;
        try {
            token = SecretValue.resolve(nxs.getToken(), dataPath);
        } catch (IOException unreadable) {
            throw new IOException("nethernet.signaling.nxs.token: " + unreadable.getMessage(), unreadable);
        }
        return ProviderRuntimeConfiguration.Settings.builder(nxs.getEndpoint())
                .token(token)
                .advertiseAddresses(settings.getAdvertiseAddresses())
                .assistedJoins(advanced.isAssistedJoins())
                .connectionChecks(advanced.isConnectionChecks())
                .stunDiscovery(settings.isStunDiscovery())
                .deregisterOnShutdown(advanced.isDeregisterOnShutdown())
                .location(location)
                .bedrockProtocols(bedrockProtocols())
                .gameOutcomes(true)
                .proxyProtocolRelays(relays)
                .build();
    }

    /**
     * Every protocol a login is accepted with, which is every {@link ProtocolVersion}, and the
     * newest display version.
     */
    static ProviderClient.BedrockProtocols bedrockProtocols() {
        List<Integer> accepted = Arrays.stream(ProtocolVersion.values()).map(ProtocolVersion::getProtocol)
                .distinct().sorted().toList();
        List<String> names = ProtocolVersion.latest().getDisplayNames();
        return new ProviderClient.BedrockProtocols(accepted, names.get(names.size() - 1));
    }

    private void announceOperatorAction(Optional<ProviderClient.OperatorAction> action) {
        try {
            String message = this.storeOperatorAction(action);
            if (message != null) {
                log.info(message);
            }
        } catch (IOException e) {
            log.warn("Could not keep the signaling service's link in {}: {}", this.operatorLinkFile(), e.getMessage());
        }
    }

    /**
     * Keeps the provider's link for the operator, such as one to claim this proxy, in the nethernet folder. See
     * {@link #storeOperatorAction(Path, Optional)}.
     */
    public String storeOperatorAction(Optional<ProviderClient.OperatorAction> action) throws IOException {
        return storeOperatorAction(this.operatorLinkFile(), action);
    }

    private Path operatorLinkFile() {
        return this.proxy.getDataPath().resolve(DATA_FOLDER).resolve(OPERATOR_LINK_FILE);
    }

    /**
     * Writes the provider's link to a file only this user can read, as it grants access to this proxy and a
     * console is often logged. With no link the file is removed. The provider and wdnethernet action can both
     * write it, so one waits for the other.
     *
     * @return the provider's text and where its link is, or null when there is none
     */
    public static synchronized String storeOperatorAction(Path file, Optional<ProviderClient.OperatorAction> action) throws IOException {
        Files.deleteIfExists(file);
        if (action.isEmpty()) {
            return null;
        }
        ProviderClient.OperatorAction value = action.get();
        Files.createDirectories(file.getParent());
        if (file.getFileSystem().supportedFileAttributeViews().contains("posix")) {
            Files.createFile(file, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
        }
        Files.writeString(file, value.text() + "\n" + value.url() + "\nExpires " + Instant.ofEpochMilli(value.expiresAt()) + "\n");
        return value.text() + " The link is in " + file + ".";
    }

    private ProviderClient.Health health() {
        return health(this.proxy.callPingEvent(null), this.proxy.getNetherNetInterface().isFull(),
                new ProviderClient.Software("WaterdogPE", WaterdogPE.version().baseVersion()));
    }

    /**
     * The counts a RakNet ping shows, which the provider lists. Like built-in signaling, only an
     * explicit limit turns players away, see {@link NetherNetInterface#isFull()}.
     */
    static ProviderClient.Health health(ProxyPingEvent ping, boolean full, ProviderClient.Software software) {
        return new ProviderClient.Health(!full, bounded(ping.getMaximumPlayerCount()), software,
                new ProviderClient.ConnectedPlayers(bounded(ping.getPlayerCount()), System.currentTimeMillis()));
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
                ? "Registered with the external signaling service. Players can join at " + address
                        + " (instance " + instanceId + ")."
                : "Registered with the external signaling service (instance " + instanceId + "). "
                        + "The addresses players join with are managed by the service.";
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
     * Asked again on every check, so what plugins change in the ping reaches the provider.
     */
    private ServerStatus collectStatus() {
        return status(this.proxy.callPingEvent(null));
    }

    /**
     * What the provider lists for this proxy, the same thing a RakNet ping shows.
     */
    static ServerStatus status(ProxyPingEvent ping) {
        return new ServerStatus(ping.getMotd(), ping.getSubMotd(), bounded(ping.getMaximumPlayerCount()),
                NetherNetInterface.gameType(ping.getGameType()));
    }

    /**
     * Keeps a count within what the provider takes.
     */
    private static int bounded(int count) {
        return Math.max(0, Math.min(count, 1_000_000));
    }
}
