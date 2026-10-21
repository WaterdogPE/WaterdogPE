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
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

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
    /** The media port of the primary listener, 0 until NetherNet starts. */
    private volatile int mediaPort;

    public NetherNetInterface(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Override
    public void start(InetSocketAddress address) throws NetworkStartupException {
        if (!this.proxy.getConfiguration().getTransport().nethernet()) {
            return;
        }
        NetherNetSettings settings = this.proxy.getNetherNetSettings();

        int icePort = this.icePort(address, settings);
        if (icePort == address.getPort() && this.proxy.getConfiguration().getTransport().raknet()) {
            log.error("nethernet.webrtc_port {} is the listener port, which RakNet already holds, so NetherNet does "
                    + "not start and connections will fall back to RakNet", icePort);
            return;
        }
        if (this.mediaPort == 0) {
            this.mediaPort = icePort;
        }

        try {
            this.init(settings);
        } catch (Throwable t) {
            this.fail("Unable to initialize NetherNet. The native ICE and DTLS library may be missing for this platform", t);
            return;
        }

        InetSocketAddress signalingAddress = this.signalingAddress(address, settings);
        SignalingSettings.SignalingMode mode = settings.getSignaling().getMode();

        if (mode.nxs()) {
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
                return;
            }
        }

        NetherNetHTTPServerSignaling signaling;
        try {
            signaling = this.signaling(settings.getSignaling(), address, icePort);
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

            this.bindings.add(new Binding(signaling, channel));
            this.running = true;
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
            log.info("NetherNet is bound but serves no endpoint, offers must arrive through the signaling API");
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
    private NetherNetHTTPServerSignaling signaling(SignalingSettings settings, InetSocketAddress address, int icePort)
            throws Exception {
        List<String> stunServers = stunServers(settings);
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
     * The port ICE gathers on, one socket for every peer. See {@link NetherNetSettings#resolveWebrtcPort}.
     */
    private int icePort(InetSocketAddress listener, NetherNetSettings settings) {
        Set<Integer> serverPorts = this.proxy.getConfiguration().getServerList().values().stream()
                .map(server -> server.getAddress().getPort())
                .collect(Collectors.toSet());
        return settings.resolveWebrtcPort(listener.getPort(), this.proxy.getConfiguration().getTransport().raknet(),
                serverPorts);
    }

    @Override
    public PongData advertisement(InetSocketAddress client) {
        ProxyConfig config = this.proxy.getConfiguration();

        // Reusing the ping event means plugins that already adjust the MOTD and player counts keep
        // working on NetherNet.
        ProxyPingEvent ping = new ProxyPingEvent(
                config.getMotd(),
                config.getSubMotd(),
                "Survival",
                "MCPE",
                ProtocolVersion.latest().getMinecraftVersion(),
                this.proxy.getPlayerManager().getPlayers().values(),
                config.getMaxPlayerCount(),
                client);
        this.proxy.getEventManager().callEvent(ping);

        return new PongData.Builder()
                .setServerName(ping.getMotd())
                .setProtocol(ProtocolVersion.latest().getProtocol())
                .setVersion(ping.getVersion())
                .setLevelName(ping.getSubMotd())
                .setPlayerCount(ping.getPlayerCount())
                .setMaxPlayerCount(ping.getMaximumPlayerCount())
                .setGameType(gameType(ping.getGameType()))
                // Offline mode means the proxy signs its own chains rather than the auth service
                .setSelfSignedAuth(!config.isOnlineMode())
                .build();
    }

    private static int gameType(String name) {
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

    private record Binding(NetherNetHTTPServerSignaling signaling, Channel channel) {
    }

    /**
     * @param pendingJoins offers answered but not yet connected, which is where a blocked ICE port
     *                     shows up first
     * @param networkId    the NetworkID clients address this endpoint by
     */
    public record SignalingInfo(SocketAddress bind, boolean active, int pendingJoins, String networkId) {
    }
}
