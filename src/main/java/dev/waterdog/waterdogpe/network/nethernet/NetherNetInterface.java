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

import org.cloudburstmc.netty.channel.nethernet.NetherNetChannelFactory;
import org.cloudburstmc.netty.channel.nethernet.signaling.NetherNetHTTPSignaling;
import org.cloudburstmc.netty.channel.nethernet.signaling.NetherNetServerSignaling.PongData;
import org.cloudburstmc.netty.channel.nethernet.signaling.NetherNetSignaling;
import org.cloudburstmc.netty.util.nethernet.TokenTrust;
import org.cloudburstmc.netty.util.nethernet.TrustedProxies;
import org.cloudburstmc.netty.channel.nethernet.config.NetherChannelOption;
import org.cloudburstmc.netty.util.nethernet.NetherNetLogging;
import org.cloudburstmc.netty.util.nethernet.ServerIdentity;
import tel.schich.libdatachannel.LibDataChannelArchDetect;
import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.event.defaults.ProxyPingEvent;
import dev.waterdog.waterdogpe.network.NetworkInterface;
import dev.waterdog.waterdogpe.network.connection.codec.initializer.NetherNetServerSessionInitializer;
import dev.waterdog.waterdogpe.network.protocol.ProtocolVersion;
import dev.waterdog.waterdogpe.utils.ThreadFactoryBuilder;
import dev.waterdog.waterdogpe.utils.config.proxy.HttpsSettings;
import dev.waterdog.waterdogpe.utils.config.proxy.NetherNetSettings;
import dev.waterdog.waterdogpe.utils.config.proxy.ProxyConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.ChannelInitializer;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.extern.log4j.Log4j2;

import tel.schich.libdatachannel.PeerConnectionConfiguration;

import java.net.InetAddress;
import java.nio.file.Path;
import java.net.InetSocketAddress;
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

    private EventLoopGroup signalingGroup;

    private static final int GAME_TYPE_SURVIVAL = 0;
    private static final int GAME_TYPE_CREATIVE = 1;
    private static final int GAME_TYPE_ADVENTURE = 2;

    private final ProxyServer proxy;
    private final List<Binding> bindings = new ObjectArrayList<>();
    /** Stable for the lifetime of the process, like the BDS advertisement nonce. */
    private ServerIdentity identity;
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
            log.error("Unable to initialise NetherNet, connections will fall back to RakNet. The native "
                    + "ICE and DTLS library may be missing for this platform", t);
            return;
        }

        InetSocketAddress signalingAddress = this.signalingAddress(address, settings);
        int icePort = this.icePort(address, settings);

        NetherNetHTTPSignaling signaling;
        try {
            signaling = this.signaling(settings, icePort);
        } catch (Exception e) {
            log.error("Failed to configure NetherNet signaling, connections will fall back to RakNet", e);
            return;
        }

        try {
            ServerBootstrap bootstrap = new ServerBootstrap()
                    // The signaling endpoint binds an NIO channel, so it needs a matching loop.
                    // Nothing hot runs here: the media is handled by the native ICE and DTLS stack.
                    .group(this.signalingGroup())
                    .channelFactory(NetherNetChannelFactory.server(signaling))
                    .option(NetherChannelOption.NETHER_SERVER_RTC_HANDSHAKE_TIMEOUT_SECONDS, settings.getHandshakeTimeout())
                    .handler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel channel) {
                            if (icePort <= 0) {
                                return;
                            }
                            // Runs before the first connection, so every peer sees the pinned port
                            ChannelConfig options = channel.config();
                            options.setOption(NetherChannelOption.NETHER_PEER_CONNECTION_CONFIG,
                                    pinIce(options.getOption(NetherChannelOption.NETHER_PEER_CONNECTION_CONFIG),
                                            address.getAddress(), icePort));
                        }
                    })
                    .childHandler(new NetherNetServerSessionInitializer(this.proxy));

            // The channel binds signaling over TCP here; RakNet keeps the UDP side of the same port
            Channel channel = bootstrap.bind(signalingAddress).syncUninterruptibly().channel();

            this.bindings.add(new Binding(signaling, channel));
            this.running = true;
        } catch (Exception e) {
            signaling.close();
            log.error("Failed to start NetherNet on {}, connections will fall back to RakNet", address, e);
            return;
        }

        if (settings.signalingMode() == NetherNetSettings.SignalingMode.BUILTIN) {
            // TLS is served on the same port as plaintext, so both schemes reach it when configured
            String scheme = settings.getHttps().enabled() ? "https and http" : "http";
            log.info("NetherNet signaling listening on tcp/{} over {}{}", signalingAddress.getPort(), scheme,
                    icePort > 0 ? ", with WebRTC on udp/" + icePort : "");
        } else {
            log.info("NetherNet is bound but serves no endpoint, offers must arrive through the signaling API");
        }
    }

    /**
     * The configured STUN and TURN servers, as one entry carrying every URL. Credentials belong in
     * the URL, which is the only place the configuration has to put them.
     */
    private static List<NetherNetSignaling.IceServerInfo> iceServers(NetherNetSettings settings) {
        List<String> urls = settings.getIceServers();
        if (urls.isEmpty()) {
            return List.of();
        }
        return List.of(new NetherNetSignaling.IceServerInfo.Builder().setUrls(List.copyOf(urls)).build());
    }

    /**
     * Builds the signaling endpoint from the configuration.
     */
    private NetherNetHTTPSignaling signaling(NetherNetSettings settings, int icePort) throws Exception {
        NetherNetHTTPSignaling.Builder builder = new NetherNetHTTPSignaling.Builder()
                .setIdentity(this.identity)
                .setServeHttp(settings.signalingMode() == NetherNetSettings.SignalingMode.BUILTIN)
                .setTrustedProxies(TrustedProxies.parse(settings.getTrustedProxies()))
                .setProxyProtocol(settings.isProxyProtocol())
                .setAdvertisedAddresses(settings.getAdvertiseAddresses())
                .setIceServers(iceServers(settings))
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
            String password = https.getPassword();
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
     * Sends ICE to its own port, because the signaling port is only free on the TCP side.
     */
    private static PeerConnectionConfiguration pinIce(PeerConnectionConfiguration config, InetAddress host, int port) {
        // A wildcard bind is left unset so ICE keeps gathering on every interface
        if (host != null && !host.isAnyLocalAddress()) {
            config = config.withBindAddress(host);
        }
        return config
                .withEnableIceUdpMux(true)
                .withPortRangeBegin(port)
                .withPortRangeEnd(port);
    }

    /**
     * The signaling port defaults to the listener port, which is where clients look for it. An
     * override only applies to the primary bind, additional ports always mirror their own.
     */
    private InetSocketAddress signalingAddress(InetSocketAddress address, NetherNetSettings settings) {
        int port = settings.getSignalingPort() > 0 && this.bindings.isEmpty()
                ? settings.getSignalingPort() : address.getPort();
        return new InetSocketAddress(address.getAddress(), port);
    }

    private void init(NetherNetSettings settings) throws Exception {
        if (this.identity != null) {
            return;
        }

        // Loads the native built for this platform out of the bundled set.
        LibDataChannelArchDetect.initialize();

        // The native ICE and DTLS stack logs through slf4j once a threshold is set.
        NetherNetLogging.setNativeLogLevel(System.getProperty("waterdog.nethernetLog", "WARN"));

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

    private synchronized EventLoopGroup signalingGroup() {
        if (this.signalingGroup == null) {
            this.signalingGroup = new NioEventLoopGroup(1, ThreadFactoryBuilder.builder()
                    .format("NetherNet Signaling - #%d").build());
        }
        return this.signalingGroup;
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
        int limit = this.proxy.getNetherNetSettings().getMaxConnections();
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
    public boolean acceptsConnections() {
        return this.running && !this.bindings.isEmpty() && !this.isFull();
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

        if (this.signalingGroup != null) {
            this.signalingGroup.shutdownGracefully();
            this.signalingGroup = null;
        }
    }

    @Override
    public boolean isRunning() {
        return this.running;
    }

    private record Binding(NetherNetHTTPSignaling signaling, Channel channel) {
    }
}
