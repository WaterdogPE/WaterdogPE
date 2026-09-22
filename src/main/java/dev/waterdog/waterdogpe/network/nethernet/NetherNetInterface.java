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
import dev.waterdog.waterdogpe.network.connection.codec.initializer.NetherNetServerSessionInitializer;
import dev.waterdog.waterdogpe.network.protocol.ProtocolVersion;
import dev.waterdog.waterdogpe.utils.config.proxy.HttpsSettings;
import dev.waterdog.waterdogpe.utils.config.proxy.NetherNetSettings;
import dev.waterdog.waterdogpe.utils.config.proxy.ProxyConfig;
import dev.waterdog.waterdogpe.network.NetworkMetrics;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.extern.log4j.Log4j2;


import java.net.InetAddress;
import java.nio.file.Path;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Accepts NetherNet connections. Runs alongside {@link dev.waterdog.waterdogpe.network.RakNetInterface}
 * rather than replacing it, so a client can reach the proxy over either transport.
 * <p>
 * Signaling is HTTP on TCP, which does not collide with the RakNet UDP bind, so both transports
 * share one advertised port. Media needs its own UDP port, because RakNet holds the listener port
 * with SO_REUSEPORT, and every peer is multiplexed over it. See
 * {@link NetherNetSettings#getUdpPort()}.
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

    public NetherNetInterface(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Override
    public void start(InetSocketAddress address) {
        NetherNetSettings settings = this.proxy.getNetherNetSettings();
        if (!settings.enabled()) {
            return;
        }

        try {
            this.init(settings);
        } catch (Throwable t) {
            // Clients that cannot reach a signaling endpoint fall back to RakNet on their own, so a
            // NetherNet failure degrades the proxy instead of taking it down. Logged as an error
            // anyway, because nothing else reports that the transport is missing.
            log.error("Unable to initialize NetherNet, connections will fall back to RakNet. The native "
                    + "ICE and DTLS library may be missing for this platform", t);
            return;
        }

        InetSocketAddress signalingAddress = this.signalingAddress(address, settings);
        int icePort = this.icePort(address, settings);

        if (settings.signalingMode().nxs()) {
            this.startProvider(settings, address, icePort);
            if (settings.signalingMode() == NetherNetSettings.SignalingMode.NXS) {
                // The provider brings its own endpoint and admits peers onto it
                this.running = true;
                return;
            }
        }

        NetherNetHTTPServerSignaling signaling;
        try {
            signaling = this.signaling(settings, address, icePort);
        } catch (Exception e) {
            log.error("Failed to configure NetherNet signaling, connections will fall back to RakNet", e);
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
                    .childHandler(new NetherNetServerSessionInitializer(this.proxy));
            if (icePort > 0) {
                // Media on its own port, because the signaling port is only free on the TCP side
                bootstrap.option(NetherChannelOption.NETHER_SERVER_ICE_ADDRESS,
                        new InetSocketAddress(address.getAddress(), icePort));
            }

            // The channel binds signaling over TCP here; RakNet keeps the UDP side of the same port
            Channel channel = bootstrap.bind(signalingAddress).syncUninterruptibly().channel();

            this.bindings.add(new Binding(signaling, channel));
            this.running = true;
        } catch (Exception e) {
            signaling.close();
            log.error("Failed to start NetherNet on {}, connections will fall back to RakNet", address, e);
            return;
        }

        if (settings.signalingMode().builtin()) {
            // TLS is served on the same port as plaintext, so both schemes reach it when configured
            String scheme = settings.getHttps().enabled() ? "https and http" : "http";
            log.info("NetherNet signaling listening on tcp/{} over {}{}", signalingAddress.getPort(), scheme,
                    icePort > 0 ? ", with WebRTC on udp/" + icePort : "");
        } else {
            log.info("NetherNet is bound but serves no endpoint, offers must arrive through the signaling API");
        }
    }

    /**
     * Registers with the provider, which then admits players onto a port of its own. A provider that
     * cannot be reached leaves the rest of the proxy alone, the same way signaling does.
     */
    private void startProvider(NetherNetSettings settings, InetSocketAddress address, int icePort) {
        if (icePort <= 0) {
            log.error("NetherNet provider registration needs a fixed udp_port, no players will arrive through it");
            return;
        }
        NetherNetProvider provider = new NetherNetProvider(this.proxy);
        try {
            provider.start(settings, address, icePort);
            this.provider = provider;
        } catch (Throwable t) {
            provider.close();
            log.error("Unable to register with the NetherNet provider, no players will arrive through it", t);
        }
    }

    /**
     * The STUN and TURN servers from the system property, as one entry carrying every URL.
     * Credentials belong in the URL, which is the only place they have to go.
     */
    private static List<IceServerInfo> iceServers() {
        List<String> urls = NetherNetProperties.ICE_SERVERS;
        if (urls.isEmpty()) {
            return List.of();
        }
        return List.of(new IceServerInfo.Builder().setUrls(List.copyOf(urls)).build());
    }

    /**
     * Builds the signaling endpoint from the configuration.
     */
    private NetherNetHTTPServerSignaling signaling(NetherNetSettings settings, InetSocketAddress address, int icePort)
            throws Exception {
        NetherNetHTTPServerSignaling.Builder builder = new NetherNetHTTPServerSignaling.Builder()
                .setIdentity(this.identity)
                .setServeHttp(settings.signalingMode().builtin())
                .setTrustedProxies(TrustedProxies.parse(settings.getTrustedProxies()))
                .setProxyProtocol(settings.isProxyProtocol())
                .setAdvertisedAddresses(advertisedAddresses(address, settings))
                .setIceServers(iceServers())
                // RakNet holds the UDP side of the signaling port, so ICE never uses it
                .setIceOnLocalPort(false)
                // A peer may be another proxy signing its own assertion, which no auth service issued
                .setTokenTrust(TokenTrust.ANY)
                .setMotdProvider((host, client) -> this.advertisement(client))
                .setPlayerFilter((host, player) -> this.acceptsConnections());

        HttpsSettings https = settings.getHttps();
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

        // The native ICE and DTLS stack logs through slf4j once a threshold is set.
        NetherNetLogging.setNativeLogLevel(NetherNetProperties.NATIVE_LOG_LEVEL);

        this.identity = ProxyIdentity.identity(this.proxy);
        log.info("NetherNet identifies this operator to players as {}", ProxyIdentity.domain(this.proxy));
    }

    /**
     * Which local addresses may appear in the answer.
     * <p>
     * WebRTC gathers a candidate on every interface it can see, which on a host network includes
     * the container and overlay addresses of the machine. Those are unreachable from the internet
     * and every one of them costs the client a round of connectivity checks before it gives up, so
     * a proxy bound to one address advertises only that address unless told otherwise.
     */
    private static Set<String> advertisedAddresses(InetSocketAddress address, NetherNetSettings settings) {
        if (!settings.getAdvertiseAddresses().isEmpty()) {
            return Set.copyOf(settings.getAdvertiseAddresses());
        }
        InetAddress bound = address.getAddress();
        if (bound == null || bound.isAnyLocalAddress()) {
            return Set.of();
        }
        return Set.of(bound.getHostAddress());
    }

    /**
     * The port ICE gathers on. A dedicated port lets the transport multiplex every peer over one
     * socket; without one, ICE falls back to an ephemeral port per peer, because the signaling port
     * belongs to RakNet on the UDP side.
     */
    private int icePort(InetSocketAddress listener, NetherNetSettings settings) {
        int port = settings.getUdpPort();
        if (port == listener.getPort()) {
            log.error("nethernet.udp_port {} is the listener port, which RakNet already holds. "
                    + "Falling back to ephemeral ports", port);
            return 0;
        }
        return Math.max(port, 0);
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
        return this.bindings.get(0).signaling().acceptOffer(networkId, offer, clientAddress, null);
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
