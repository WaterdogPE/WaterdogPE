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

package dev.waterdog.waterdogpe.network.connection;

import dev.waterdog.waterdogpe.network.connection.client.BedrockClientConnection;
import dev.waterdog.waterdogpe.network.connection.client.ClientConnection;
import dev.waterdog.waterdogpe.network.connection.peer.ProxiedBedrockPeer;
import dev.waterdog.waterdogpe.network.protocol.ProtocolVersion;
import dev.waterdog.waterdogpe.network.protocol.user.LoginData;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.cloudburstmc.netty.channel.nethernet.NetherNetChannel;
import org.cloudburstmc.netty.channel.nethernet.NetherNetChildChannel;
import org.cloudburstmc.netty.channel.nethernet.config.NetherNetAddress;
import org.cloudburstmc.netty.channel.raknet.RakChannel;
import org.cloudburstmc.netty.channel.raknet.config.RakChannelOption;
import org.cloudburstmc.netty.handler.codec.raknet.common.RakSessionCodec;
import org.cloudburstmc.netty.signaling.admission.AdmissionPrincipal;
import org.cloudburstmc.netty.util.nethernet.PlayerInfo;
import org.cloudburstmc.netty.util.nethernet.TransportIdentityBinding;
import org.cloudburstmc.protocol.bedrock.codec.v428.Bedrock_v428;
import org.cloudburstmc.protocol.bedrock.netty.codec.compression.BatchCompression;
import org.cloudburstmc.protocol.bedrock.netty.codec.compression.CompressionCodec;
import org.cloudburstmc.protocol.bedrock.netty.codec.encryption.BedrockEncryptionEncoder;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;

/**
 * Describes what a player's connection runs over, on both legs of the proxy.
 * <p>
 * Every fact here comes from the live channel rather than the configuration, so the report shows
 * what a session negotiated, not what it was meant to. That is what makes it useful when a client
 * shows nothing about its own transport.
 */
public final class ConnectionDiagnostics {

    private ConnectionDiagnostics() {
    }

    /**
     * @param note what the value affects, or null when the label says enough
     */
    public record Line(String label, String value, String note) {
        public Line(String label, String value) {
            this(label, value, null);
        }
    }

    /**
     * The client itself and the leg between it and the proxy.
     */
    public static List<Line> upstream(ProxiedPlayer player) {
        List<Line> lines = new ObjectArrayList<>();
        LoginData login = player.getLoginData();
        ProtocolVersion protocol = player.getProtocol();

        lines.add(new Line("Client", protocol.getMinecraftVersion() + " (protocol " + protocol.getProtocol() + ")"));
        String device = player.getDevicePlatform() + (player.getDeviceModel() == null ? "" : " " + player.getDeviceModel());
        lines.add(new Line("Device", device));
        lines.add(new Line("Xbox", login.isXboxAuthed() ? "authenticated" : "not authenticated",
                login.isXboxAuthed() ? null : "offline mode, identity is unverified"));
        if (login.getJoinHostname() != null && !login.getJoinHostname().isBlank()) {
            lines.add(new Line("Joined via", login.getJoinHostname()));
        }

        ProxiedBedrockPeer peer = player.getConnection().getPeer();
        transport(lines, peer.getChannel(), player.getConnection().getSocketAddress());
        codecs(lines, peer.getChannel(), peer.getTransportProfile(), protocol);
        return lines;
    }

    /**
     * The leg between the proxy and a downstream server.
     */
    public static List<Line> downstream(ClientConnection connection, ProtocolVersion protocol) {
        List<Line> lines = new ObjectArrayList<>();
        ServerInfo server = connection.getServerInfo();
        lines.add(new Line("Server", server.getServerName() + " (" + server.getServerType().getIdentifier() + ")"));
        lines.add(new Line("Configured", String.valueOf(server.getAddress())));

        if (connection instanceof BedrockClientConnection bedrock) {
            transport(lines, bedrock.getChannel(), connection.getSocketAddress());
            codecs(lines, bedrock.getChannel(), profile(bedrock, protocol), protocol);
        } else {
            lines.add(new Line("Transport", connection.getClass().getSimpleName()));
            lines.add(new Line("Address", String.valueOf(connection.getSocketAddress())));
            lines.add(new Line("Ping", connection.getPing() + " ms"));
        }
        return lines;
    }

    private static TransportProfile profile(BedrockClientConnection connection, ProtocolVersion protocol) {
        return connection.supportsEncryption() ? TransportProfile.raknet(protocol.getRaknetVersion()) : TransportProfile.NETHERNET;
    }

    private static void transport(List<Line> lines, Channel channel, SocketAddress address) {
        if (channel instanceof RakChannel rak) {
            rakNet(lines, rak, address);
        } else if (channel instanceof NetherNetChannel netherNet) {
            netherNet(lines, netherNet, address);
        } else {
            lines.add(new Line("Transport", channel == null ? "unknown" : channel.getClass().getSimpleName()));
            lines.add(new Line("Address", String.valueOf(address)));
        }
    }

    private static void rakNet(List<Line> lines, RakChannel channel, SocketAddress address) {
        lines.add(new Line("Transport", "RakNet", "UDP, the default Bedrock transport"));
        lines.add(new Line("Address", String.valueOf(address)));
        Integer version = channel.config().getOption(RakChannelOption.RAK_PROTOCOL_VERSION);
        if (version != null) {
            lines.add(new Line("RakNet protocol", String.valueOf(version)));
        }
        RakSessionCodec session = channel.rakPipeline().get(RakSessionCodec.class);
        if (session == null) {
            return;
        }
        lines.add(new Line("MTU", session.getMtu() + " bytes", "largest datagram, bigger batches are split"));
        lines.add(new Line("Ping", session.getPing() + " ms", "this leg only"));
    }

    private static void netherNet(List<Line> lines, NetherNetChannel channel, SocketAddress address) {
        lines.add(new Line("Transport", "NetherNet", "WebRTC data channels inside DTLS"));

        AdmissionPrincipal admission = channel.attr(AdmissionPrincipal.KEY).get();
        PlayerInfo signaled = channel.attr(NetherNetChildChannel.PLAYER_INFO).get();
        if (admission != null) {
            lines.add(new Line("Signaling", "provider admission", "ticket " + admission.ticketId()));
            lines.add(new Line("Network id", admission.networkId()));
        } else if (signaled != null) {
            lines.add(new Line("Signaling", "builtin http", "offer arrived on the proxy's own endpoint"));
            lines.add(new Line("Network id", signaled.networkId()));
        } else if (channel.remoteAddress() instanceof NetherNetAddress remote) {
            lines.add(new Line("Signaling", "outbound", "the proxy sent the offer"));
            lines.add(new Line("Network id", remote.getNetworkId()));
        } else if (channel.parent() != null) {
            lines.add(new Line("Signaling", "external", "offer was fed in through the signaling API"));
        }
        if (address instanceof InetSocketAddress) {
            lines.add(new Line("Address", String.valueOf(address), "as seen at signaling"));
        }

        NetherNetChannel.Path path = channel.selectedPath();
        if (path == null) {
            lines.add(new Line("Path", "not connected yet", "ICE has not selected a candidate pair"));
        } else {
            lines.add(new Line("Path", candidate(path.remote(), path.remoteType()) + " to "
                    + candidate(path.local(), path.localType()), pathNote(path)));
            InetAddress signaledIp = signaled == null || signaled.remoteAddress() == null ? null : signaled.remoteAddress().getAddress();
            if (signaledIp != null && path.remote() != null && !signaledIp.equals(path.remote().getAddress())) {
                lines.add(new Line("Path address", "differs from the signaling address",
                        "a relay, a VPN, or a different network for media"));
            }
        }

        lines.add(new Line("Ping", channel.getPing() + " ms", "SCTP round trip, this leg only"));
        int maxMessage = channel.remoteMaxMessageSize();
        if (maxMessage > 0) {
            lines.add(new Line("Max message", maxMessage + " bytes", "bigger batches are segmented"));
        }

        if (channel.parent() != null) {
            lines.add(new Line("Identity binding", binding(TransportIdentityBinding.state(channel))));
        }
    }

    private static String candidate(InetSocketAddress address, String type) {
        return address + (type == null ? "" : " (" + type + ")");
    }

    private static String pathNote(NetherNetChannel.Path path) {
        if ("relay".equals(path.remoteType()) || "relay".equals(path.localType())) {
            return "traffic passes through a TURN server";
        }
        if ("srflx".equals(path.remoteType()) || "srflx".equals(path.localType())) {
            return "direct, through NAT";
        }
        if ("host".equals(path.remoteType()) && "host".equals(path.localType())) {
            return "direct, no NAT in between";
        }
        return "direct";
    }

    private static String binding(TransportIdentityBinding.State state) {
        return switch (state) {
            case NONE -> "none, the transport validated no identity";
            case PENDING -> "pending, no login checked against it yet";
            case ACCEPTED -> "login key matched the signaling identity";
            case REJECTED -> "refused or lapsed";
        };
    }

    private static void codecs(List<Line> lines, Channel channel, TransportProfile profile, ProtocolVersion protocol) {
        if (channel == null) {
            return;
        }
        if (channel.pipeline().get(BedrockEncryptionEncoder.class) != null) {
            boolean ctr = protocol.getProtocol() >= Bedrock_v428.CODEC.getProtocolVersion();
            lines.add(new Line("Encryption", ctr ? "AES-CTR" : "AES-CFB8", "Bedrock packet encryption"));
        } else if (!profile.supportsEncryption()) {
            lines.add(new Line("Encryption", "none", "DTLS protects the transport instead"));
        } else {
            lines.add(new Line("Encryption", "none"));
        }

        ChannelHandler handler = channel.pipeline().get(CompressionCodec.NAME);
        if (handler instanceof CompressionCodec codec) {
            BatchCompression compression = codec.getStrategy().getDefaultCompression();
            boolean prefixed = protocol.isAfterOrEqual(ProtocolVersion.MINECRAFT_PE_1_20_60);
            lines.add(new Line("Compression", compression.getAlgorithm() + " level " + compression.getLevel(),
                    prefixed ? "algorithm byte on every batch" : "fixed per session"));
        } else {
            lines.add(new Line("Compression", "not negotiated yet"));
        }
        lines.add(new Line("Framing", "RakNet protocol " + profile.codecVersion() + " rules",
                "how batches are framed and compressed"));
    }
}
