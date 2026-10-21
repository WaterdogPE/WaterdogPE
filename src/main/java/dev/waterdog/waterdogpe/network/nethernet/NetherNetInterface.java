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

import org.cloudburstmc.netty.channel.nethernet.signaling.IceServerInfo;
import org.cloudburstmc.netty.channel.nethernet.signaling.JoinRefusal;
import org.cloudburstmc.netty.channel.nethernet.NetherNetChannelFactory;
import org.cloudburstmc.netty.channel.nethernet.signaling.NetherNetHTTPServerSignaling;
import org.cloudburstmc.netty.channel.nethernet.signaling.PongData;
import org.cloudburstmc.netty.util.nethernet.TokenTrust;
import org.cloudburstmc.netty.util.nethernet.TrustedProxies;
import org.cloudburstmc.netty.channel.nethernet.config.DefaultNetherServerChannelConfig;
import org.cloudburstmc.netty.channel.nethernet.config.NetherChannelOption;
import org.cloudburstmc.netty.channel.nethernet.config.NetherServerMetrics;
import org.cloudburstmc.netty.util.nethernet.NetherNetLogging;
import org.cloudburstmc.netty.util.nethernet.SecretValue;
import org.cloudburstmc.netty.util.nethernet.OperatorIdentity;
import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.event.defaults.ProxyPingEvent;
import dev.waterdog.waterdogpe.network.NetworkInterface;
import dev.waterdog.waterdogpe.network.NetworkStartupException;
import dev.waterdog.waterdogpe.network.connection.codec.initializer.NetherNetServerSessionInitializer;
import dev.waterdog.waterdogpe.network.protocol.ProtocolVersion;
import dev.waterdog.waterdogpe.utils.config.proxy.HttpsSettings;
import dev.waterdog.waterdogpe.utils.config.proxy.NetherNetSettings;
import dev.waterdog.waterdogpe.utils.config.proxy.ProxyConfig;
import dev.waterdog.waterdogpe.utils.config.proxy.SignalingSettings;
import dev.waterdog.waterdogpe.network.NetworkMetrics;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.extern.log4j.Log4j2;
import tel.schich.libdatachannel.PeerConnectionConfiguration;


import java.net.InetAddress;
import java.nio.file.Path;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Accepts NetherNet connections, next to {@link dev.waterdog.waterdogpe.network.RakNetInterface} or
 * on its own, as {@link ProxyConfig#getTransport()} says.
 * <p>
 * Signaling is HTTP on TCP, which does not collide with the RakNet UDP bind, so both transports
 * share one advertised port. Media needs its own UDP port while RakNet holds the listener port
 * with SO_REUSEPORT, and every peer is multiplexed over it. See
 * {@link NetherNetSettings#getWebrtcPort()}.
 */
@Log4j2
public class NetherNetInterface implements NetworkInterface, SignalingService {

    private static final int GAME_TYPE_SURVIVAL = 0;
    private static final int GAME_TYPE_CREATIVE = 1;
    private static final int GAME_TYPE_ADVENTURE = 2;

    private final ProxyServer proxy;
    private final List<Binding> bindings = new ObjectArrayList<>();

    private volatile NetherServerMetrics serverMetrics;
    private volatile NetherNetProvider provider;
    /** Stable for the lifetime of the process, like the BDS advertisement nonce. */
    private OperatorIdentity identity;
    private boolean running;
    /** Whether the listener started first, the only one that registers with NXS, has started. */
    private boolean primaryStarted;
    /** The media port of the first listener, 0 until NetherNet runs there. */
    private volatile int mediaPort;

    public NetherNetInterface(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Override
    public void start(InetSocketAddress address) throws NetworkStartupException {
        ProxyConfig config = this.proxy.getConfiguration();
        if (!config.getTransport().nethernet()) {
            return;
        }
        // The proxy starts the listener first and listener.additional_ports after it
        boolean primary = !this.primaryStarted;
        this.primaryStarted = true;
        NetherNetSettings settings = this.proxy.getNetherNetSettings();
        SignalingSettings.SignalingMode mode = settings.getSignaling().getMode();
        if (!primary && mode == SignalingSettings.SignalingMode.NXS) {
            // The provider registered once, with the first listener
            return;
        }

        List<Integer> mediaPorts = this.bindings.stream().map(Binding::mediaPort).toList();
        int icePort = settings.resolveWebrtcPort(address.getPort(), config.getTransport().raknet(),
                takenPorts(config, mediaPorts));
        if (config.getTransport().raknet() && listenerPorts(config).contains(icePort)) {
            log.error("nethernet.webrtc_port {} is a listener port, which RakNet already holds, so NetherNet does "
                    + "not start on {} and connections will fall back to RakNet", icePort, address);
            return;
        }

        try {
            this.init(settings);
        } catch (Throwable t) {
            this.fail("Unable to initialize NetherNet. The native ICE and DTLS library may be missing for this platform", t);
            return;
        }

        InetSocketAddress signalingAddress = this.signalingAddress(address, settings);

        if (mode.nxs() && primary) {
            try {
                this.startProvider(settings.getSignaling(), address, icePort);
            } catch (Throwable t) {
                if (mode == SignalingSettings.SignalingMode.NXS) {
                    this.fail("Unable to register with the NetherNet provider", t);
                    return;
                }
                log.error("Unable to register with the NetherNet provider, players only arrive through built-in signaling", t);
            }
            if (mode == SignalingSettings.SignalingMode.NXS) {
                // The provider brings its own endpoint and admits peers onto it
                this.running = true;
                this.mediaPort = icePort;
                log.info("NetherNet registers with NXS, with WebRTC on udp/{}", icePort);
                return;
            }
        }

        List<String> stunServers = stunServers(settings.getSignaling());
        NetherNetHTTPServerSignaling signaling;
        try {
            signaling = this.signaling(settings.getSignaling(), address, icePort, stunServers);
        } catch (Exception e) {
            this.fail("Failed to configure NetherNet signaling", e);
            return;
        }

        try {
            ServerBootstrap bootstrap = new ServerBootstrap()
                    // Nothing hot runs on this loop: the listener has one of its own and the
                    // media is handled by the native ICE and DTLS stack
                    .group(this.proxy.getWorkerEventLoopGroup())
                    .channelFactory(NetherNetChannelFactory.server(signaling))
                    .option(NetherChannelOption.NETHER_SERVER_RTC_HANDSHAKE_TIMEOUT_SECONDS,
                            NetherNetProperties.HANDSHAKE_TIMEOUT)
                    .option(NetherChannelOption.NETHER_SERVER_METRICS, this.serverMetrics)
                    .childHandler(new NetherNetServerSessionInitializer(this.proxy, null));
            List<URI> iceServers = iceServers();
            if (!iceServers.isEmpty()) {
                bootstrap.option(NetherChannelOption.NETHER_PEER_CONNECTION_CONFIG,
                        PeerConnectionConfiguration.DEFAULT.withIceServers(iceServers));
            }
            // One socket for the media of every peer
            bootstrap.option(NetherChannelOption.NETHER_SERVER_ICE_ADDRESS,
                    new InetSocketAddress(address.getAddress(), icePort));

            // The channel binds signaling over TCP here, media is on icePort
            Channel channel = bootstrap.bind(signalingAddress).syncUninterruptibly().channel();

            this.bindings.add(new Binding(signaling, channel, !stunServers.isEmpty(), icePort));
            this.running = true;
            if (primary) {
                this.mediaPort = icePort;
            }
        } catch (Exception e) {
            signaling.close();
            this.fail("Failed to start NetherNet on " + address, e);
            return;
        }

        if (mode.builtin()) {
            // TLS is served on the same port as plaintext, so both schemes reach it when configured
            String scheme = settings.getSignaling().getBuiltin().getHttps().enabled() ? "https and http" : "http";
            log.info("NetherNet signaling listening on tcp/{} over {}, with WebRTC on udp/{}",
                    signalingAddress.getPort(), scheme, icePort);
        } else {
            log.info("NetherNet is bound but serves no endpoint, offers must arrive through the signaling API, "
                    + "with WebRTC on udp/{}", icePort);
        }
    }

    /**
     * Registers with the provider, which then admits players onto a port of its own.
     */
    private void startProvider(SignalingSettings settings, InetSocketAddress address, int icePort) throws Exception {
        NetherNetProvider provider = new NetherNetProvider(this.proxy);
        try {
            provider.start(settings, address, icePort);
        } catch (Throwable t) {
            provider.close();
            throw t;
        }
        this.provider = provider;
    }

    /**
     * Reports what keeps NetherNet from running. Clients that cannot reach signaling fall back to
     * RakNet on their own, so with RakNet running the proxy carries on, logging it as nothing else
     * says the transport is missing. Without RakNet no one could join, so the startup fails.
     */
    private void fail(String message, Throwable cause) throws NetworkStartupException {
        if (!this.proxy.getConfiguration().getTransport().raknet()) {
            throw new NetworkStartupException(message, cause);
        }
        log.error("{}. Connections will fall back to RakNet", message, cause);
    }

    /**
     * The STUN and TURN servers from the system property, which every connection's peer uses.
     * Credentials belong in the URL, which is the only place they have to go.
     */
    private static List<URI> iceServers() {
        return new IceServerInfo.Builder().setUrls(NetherNetProperties.ICE_SERVERS).build().toUris();
    }

    /**
     * Builds the signaling endpoint from the configuration.
     */
    private NetherNetHTTPServerSignaling signaling(SignalingSettings settings, InetSocketAddress address, int icePort,
                                                   List<String> stunServers) throws Exception {
        NetherNetHTTPServerSignaling.Builder builder = new NetherNetHTTPServerSignaling.Builder()
                .setIdentity(this.identity)
                .setServeHttp(settings.getMode().builtin())
                .setTrustedProxies(TrustedProxies.parse(settings.getTrustedProxies()))
                .setProxyProtocol(settings.isProxyProtocol())
                .setAdvertisedAddresses(advertisedAddresses(address, settings, !stunServers.isEmpty()))
                .setStunServers(stunServers)
                // ICE only runs on webrtc_port, see icePort
                .setIceOnLocalPort(false)
                // A peer may be another proxy signing its own assertion, which no auth service issued
                .setTokenTrust(TokenTrust.ANY)
                .setMotdProvider((host, client, info) -> this.advertisement(client))
                .setPlayerFilter((host, player) -> this.acceptsConnections());

        HttpsSettings https = settings.getBuiltin().getHttps();
        if (https.enabled()) {
            Path certificate = this.proxy.getDataPath().resolve(https.getCertificate());
            String key = https.getPrivateKey();
            String password = SecretValue.resolve(https.getPassword(), this.proxy.getDataPath());
            if (key == null || key.isBlank()) {
                builder.setHttpsKeystore(certificate.toFile(), password == null ? "" : password);
            } else {
                builder.setHttpsPem(certificate.toFile(), this.proxy.getDataPath().resolve(key).toFile(),
                        password == null || password.isBlank() ? null : password);
            }
        }
        return builder.build();
    }

    /**
     * The STUN servers built-in signaling asks for this proxy's public address, none when configured
     * addresses already say it, which network refuses next to them.
     */
    private static List<String> stunServers(SignalingSettings settings) {
        boolean wanted = settings.isStunDiscovery() && settings.getAdvertiseAddresses().isEmpty();
        if (!wanted) {
            if (NetherNetProperties.STUN_SERVERS != null) {
                log.warn("waterdog.nethernetStunServers is ignored, as {}",
                        settings.isStunDiscovery() ? "advertise_addresses is set" : "stun_discovery is off");
            }
            return List.of();
        }
        if (NetherNetProperties.STUN_SERVERS == null) {
            return NetherNetProperties.DEFAULT_STUN_SERVERS;
        }
        log.info("NetherNet STUN servers set from system property: {}", NetherNetProperties.STUN_SERVERS.isEmpty()
                ? "none" : String.join(", ", NetherNetProperties.STUN_SERVERS));
        return NetherNetProperties.STUN_SERVERS;
    }

    /**
     * The signaling port defaults to the listener port, which is where clients look for it. An
     * override only applies to the primary bind, additional ports always mirror their own.
     */
    private InetSocketAddress signalingAddress(InetSocketAddress address, NetherNetSettings settings) {
        int port = NetherNetProperties.SIGNALING_PORT > 0 && this.bindings.isEmpty()
                ? NetherNetProperties.SIGNALING_PORT : address.getPort();
        return new InetSocketAddress(address.getAddress(), port);
    }

    private void init(NetherNetSettings settings) throws Exception {
        if (this.identity != null) {
            return;
        }

        // The native ICE and DTLS stack logs through slf4j once a threshold is set
        String level = NetherNetProperties.LOG_LEVEL;
        if (level == null) {
            NetherNetLogging.setNativeLogLevel("WARN");
        } else if (NetherNetLogging.setLogLevel(level)) {
            log.info("NetherNet log level set from system property: {}", level.trim());
        } else {
            log.warn("Invalid waterdog.nethernetLog \"{}\", expected OFF, ERROR, WARN, INFO, DEBUG or TRACE", level);
            NetherNetLogging.setNativeLogLevel("WARN");
        }

        this.identity = ProxyIdentity.identity(this.proxy);
        log.info("NetherNet identifies this operator to players as {}", ProxyIdentity.domain(this.proxy));
    }

    /**
     * Which local addresses may appear in the answer.
     * <p>
     * WebRTC gathers a candidate on every interface it can see, which on a host network includes
     * the container and overlay addresses of the machine. Those are unreachable from the internet
     * and every one of them costs the client a round of connectivity checks before it gives up, so
     * a proxy bound to one address advertises only that address, unless told otherwise or STUN
     * discovery runs, as network takes no such list next to STUN servers.
     */
    private static List<String> advertisedAddresses(InetSocketAddress address, SignalingSettings settings,
                                                    boolean stun) {
        if (!settings.getAdvertiseAddresses().isEmpty()) {
            return settings.getAdvertiseAddresses();
        }
        InetAddress bound = address.getAddress();
        if (stun || bound == null || bound.isAnyLocalAddress()) {
            return List.of();
        }
        return List.of(bound.getHostAddress());
    }

    /**
     * The ports a derived WebRTC port keeps clear of: every listener's, every server's in servers, as
     * one on this machine already holds it, and the media of the listeners started before.
     */
    static Set<Integer> takenPorts(ProxyConfig config, Collection<Integer> mediaPorts) {
        Set<Integer> taken = new HashSet<>(listenerPorts(config));
        config.getServerList().values().forEach(server -> taken.add(server.getAddress().getPort()));
        taken.addAll(mediaPorts);
        return taken;
    }

    /**
     * The ports RakNet listens on: the listener's and listener.additional_ports.
     */
    private static Set<Integer> listenerPorts(ProxyConfig config) {
        Set<Integer> ports = new HashSet<>(config.getAdditionalPorts());
        ports.add(config.getBindAddress().getPort());
        return ports;
    }

    @Override
    public PongData advertisement(InetSocketAddress client) {
        ProxyPingEvent ping = this.proxy.callPingEvent(client);
        return new PongData.Builder()
                .setServerName(ping.getMotd())
                .setProtocol(ProtocolVersion.latest().getProtocol())
                .setVersion(ping.getVersion())
                .setLevelName(ping.getSubMotd())
                .setPlayerCount(ping.getPlayerCount())
                .setMaxPlayerCount(ping.getMaximumPlayerCount())
                .setGameType(gameType(ping.getGameType()))
                // Offline mode means the proxy signs its own chains rather than the auth service
                .setSelfSignedAuth(!this.proxy.getConfiguration().isOnlineMode())
                .build();
    }

    static int gameType(String name) {
        if (name == null) {
            return GAME_TYPE_SURVIVAL;
        }
        return switch (name.toLowerCase()) {
            case "creative" -> GAME_TYPE_CREATIVE;
            case "adventure" -> GAME_TYPE_ADVENTURE;
            default -> GAME_TYPE_SURVIVAL;
        };
    }

    /**
     * Only an explicit limit applies here. The listener player count is often set for display
     * rather than capacity, and refusing signaling on it would lock out every join past the first.
     */
    boolean isFull() {
        int limit = NetherNetProperties.MAX_CONNECTIONS;
        return limit > 0 && this.proxy.getPlayerManager().getPlayers().size() >= limit;
    }

    @Override
    public CompletableFuture<String> acceptOffer(String networkId, String offer, InetSocketAddress clientAddress) {
        if (this.bindings.isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalStateException("NetherNet is not bound"));
        }
        return this.bindings.get(0).signaling().acceptOffer(networkId, offer, clientAddress, null, null);
    }

    @Override
    public JoinRefusal acceptsConnections() {
        if (!this.running || this.bindings.isEmpty()) {
            return JoinRefusal.ERROR;
        }
        if (this.isFull()) {
            return JoinRefusal.FULL;
        }
        return null;
    }

    /**
     * The provider registration, or null outside the nxs and hybrid modes.
     */
    public NetherNetProvider provider() {
        return this.provider;
    }

    /**
     * What each signaling endpoint the proxy serves itself tells players to connect to.
     */
    public List<Advertised> advertised() {
        List<Advertised> advertised = new ObjectArrayList<>(this.bindings.size());
        for (Binding binding : this.bindings) {
            List<InetSocketAddress> configured = binding.signaling().advertisedAddresses().stream()
                    // An entry without a port is on the WebRTC port
                    .map(entry -> entry.getPort() == 0 ? new InetSocketAddress(entry.getAddress(), binding.mediaPort()) : entry)
                    .toList();
            advertised.add(new Advertised(binding.channel().localAddress(), configured, binding.stun(),
                    binding.signaling().discoveredEndpoints()));
        }
        return advertised;
    }

    /**
     * One entry per signaling endpoint the proxy serves itself.
     */
    public List<SignalingInfo> signalingInfo() {
        List<SignalingInfo> info = new ObjectArrayList<>(this.bindings.size());
        for (Binding binding : this.bindings) {
            info.add(new SignalingInfo(binding.channel().localAddress(), binding.signaling().isActive(),
                    binding.signaling().pendingJoins(), binding.signaling().getLocalNetworkId()));
        }
        return info;
    }

    @Override
    public void shutdown() {
        if (!this.running) {
            return;
        }
        this.running = false;

        for (Binding binding : this.bindings) {
            if (binding.channel().isOpen()) {
                binding.channel().close().syncUninterruptibly();
            }
        }
        this.bindings.clear();
        this.primaryStarted = false;
        this.mediaPort = 0;

        if (this.provider != null) {
            this.provider.close();
            this.provider = null;
        }
    }

    @Override
    public boolean isRunning() {
        return this.running;
    }

    public int mediaPort() {
        return this.mediaPort;
    }

    @Override
    public void setNetworkMetrics(NetworkMetrics metrics) {
        this.serverMetrics = metrics == null ? null : metrics.netherServerMetrics();
        for (Binding binding : this.bindings) {
            // setOption rejects null, and clearing the metrics again has to work.
            if (binding.channel().config() instanceof DefaultNetherServerChannelConfig config) {
                config.setServerMetrics(this.serverMetrics);
            }
        }
    }

    /**
     * @param stun      whether STUN discovery runs for it
     * @param mediaPort the WebRTC port its media is on
     */
    private record Binding(NetherNetHTTPServerSignaling signaling, Channel channel, boolean stun, int mediaPort) {
    }

    /**
     * @param configured the advertised addresses. When empty, this proxy's own addresses and {@code discovered}
     * @param stun       whether STUN discovery runs
     * @param discovered the public addresses STUN discovery currently finds
     */
    public record Advertised(SocketAddress bind, List<InetSocketAddress> configured, boolean stun,
                             List<InetSocketAddress> discovered) {
    }

    /**
     * @param pendingJoins offers answered but not yet connected, which is where a blocked ICE port
     *                     shows up first
     * @param networkId    the NetworkID clients address this endpoint by
     */
    public record SignalingInfo(SocketAddress bind, boolean active, int pendingJoins, String networkId) {
    }
}
