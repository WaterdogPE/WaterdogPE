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

package dev.waterdog.waterdogpe.network.protocol.handler.upstream;

import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.WaterdogPE;
import dev.waterdog.waterdogpe.event.defaults.PlayerAuthenticatedEvent;
import dev.waterdog.waterdogpe.network.connection.codec.compression.CompressionType;
import dev.waterdog.waterdogpe.network.connection.peer.BedrockServerSession;
import dev.waterdog.waterdogpe.network.protocol.ProtocolVersion;
import dev.waterdog.waterdogpe.network.protocol.user.LoginData;
import dev.waterdog.waterdogpe.network.protocol.user.HandshakeEntry;
import dev.waterdog.waterdogpe.network.protocol.user.HandshakeUtils;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import dev.waterdog.waterdogpe.security.SecurityManager;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.codec.compat.BedrockCompat;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacketHandler;
import org.cloudburstmc.protocol.bedrock.packet.*;
import org.cloudburstmc.protocol.common.PacketSignal;

import java.net.InetSocketAddress;
import java.util.Objects;

/**
 * The Pipeline Handler handling the login handshake part of the initial connect. Will be replaced after success.
 */
public class LoginUpstreamHandler implements BedrockPacketHandler {

    private final ProxyServer proxy;
    private final BedrockServerSession session;
    private ProxiedPlayer player;

    // Whether login was allowed
    private boolean loginInitialized;
    // The compression used by this session
    // Defaults to ZLIB for older versions
    private CompressionType compression;

    public LoginUpstreamHandler(ProxyServer proxy, BedrockServerSession session) {
        this.proxy = proxy;
        this.session = session;
    }

    private void onLoginFailed(HandshakeEntry handshakeEntry, Throwable throwable, String disconnectReason) {
        String message = this.proxy.getSecurityManager().onLoginFailed(this.session.getSocketAddress(), handshakeEntry, throwable, disconnectReason);
        if (this.session.isConnected()) {
            this.session.disconnect(Objects.requireNonNullElse(message, "Login Failed"));
        }
    }

    private boolean attemptLogin() {
        if (this.loginInitialized) {
            return true;
        }
        this.loginInitialized = true;

        SecurityManager securityManager = this.proxy.getSecurityManager();
        if (!securityManager.onLoginAttempt(this.session.getSocketAddress())) {
            this.proxy.getLogger().debug("[{}] <-> Login denied", this.session.getSocketAddress());
            this.session.disconnect("Login denied");
            return false;
        }
        return true;
    }

    private ProtocolVersion checkVersion(int protocolVersion) {
        ProtocolVersion protocol = ProtocolVersion.get(protocolVersion);
        if (protocol != null) {
            return protocol;
        }

        PlayStatusPacket status = new PlayStatusPacket();
        status.setStatus((protocolVersion > WaterdogPE.version().latestProtocolVersion() ?
                PlayStatusPacket.Status.LOGIN_FAILED_SERVER_OLD :
                PlayStatusPacket.Status.LOGIN_FAILED_CLIENT_OLD));
        this.session.sendPacketImmediately(status);
        this.session.disconnect();
        this.proxy.getLogger().warning("[{}] <-> Upstream has disconnected due to incompatible protocol (protocol={})", this.session.getSocketAddress(), protocolVersion);
        return null;
    }

    @Override
    public PacketSignal handle(RequestNetworkSettingsPacket packet) {
        ProtocolVersion protocol;
        if (!this.attemptLogin() || (protocol = this.checkVersion(packet.getProtocolVersion())) == null) {
            return PacketSignal.HANDLED;
        }

        this.session.setCodec(protocol.getCodec());

        if (protocol.isBefore(ProtocolVersion.MINECRAFT_PE_1_19_30)) {
            this.session.disconnect("Illegal packet");
            this.proxy.getLogger().warning("[{}] <-> Upstream has requested network settings, but its version doesn't support it (protocol={})", this.session.getSocketAddress(), protocol.getProtocol());
            return PacketSignal.HANDLED;
        }

        this.compression = this.proxy.getConfiguration().getCompression();

        NetworkSettingsPacket settingsPacket = new NetworkSettingsPacket();
        settingsPacket.setCompressionThreshold(1);
        settingsPacket.setCompressionAlgorithm(this.compression.getBedrockAlgorithm());
        this.session.sendPacketImmediately(settingsPacket);

        if (!this.session.isSubClient()) {
            this.session.getPeer().setCompression(this.compression);
        }
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(LoginPacket packet) {
        ProtocolVersion protocol;
        if (!this.attemptLogin() || (protocol = this.checkVersion(packet.getProtocolVersion())) == null) {
            return PacketSignal.HANDLED;
        }

        BedrockCodec codec = this.session.getCodec();
        if (codec == null || codec == BedrockCompat.CODEC) {
            this.session.getPeer().setProtocol(protocol);
        }

        if (protocol.isAfterOrEqual(ProtocolVersion.MINECRAFT_PE_1_19_30) && this.compression == null) {
            this.proxy.getLogger().warning("[{}] <-> Upstream has not requested network settings (protocol={})", this.session.getSocketAddress(), protocol.getProtocol());
            this.session.disconnect("Wrong login flow");
            return PacketSignal.HANDLED;
        } else if (this.compression == null) {
            this.compression = CompressionType.ZLIB;
        }

        HandshakeEntry handshakeEntry = null;
        boolean strictAuth = this.proxy.getConfiguration().isOnlineMode();

        this.session.setLogging(WaterdogPE.version().debug());
        try {
            this.proxy.getLogger().debug("[{}] <-> Received login with authType: {} and payloadType: {}.", this.session.getSocketAddress(),
                    packet.getAuthPayload().getClass().getSimpleName(), packet.getAuthPayload().getAuthType());

            handshakeEntry = HandshakeUtils.processHandshake(this.session, packet, protocol, strictAuth);
            if (!handshakeEntry.isXboxAuthed() && strictAuth) {
                this.onLoginFailed(handshakeEntry, null, "disconnectionScreen.notAuthenticated");
                this.proxy.getLogger().info("[{}|{}] <-> Upstream has disconnected due to failed XBOX authentication!", this.session.getSocketAddress(), handshakeEntry.getDisplayName());
                return PacketSignal.HANDLED;
            }

            // Thank you Mojang: this version includes protocol changes, but protocol version was not increased.
            if (protocol.equals(ProtocolVersion.MINECRAFT_PE_1_19_60) && handshakeEntry.getClientData().has("GameVersion") &&
                    ProtocolVersion.MINECRAFT_PE_1_19_62.getMinecraftVersion().equals(handshakeEntry.getClientData().get("GameVersion").getAsString())) {
                handshakeEntry.setProtocol(protocol = ProtocolVersion.MINECRAFT_PE_1_19_62);
                this.session.getPeer().setProtocol(protocol);
            }

            this.proxy.getLogger().info("[{}|{}] <-> Upstream has connected (protocol={} version={})", this.session.getSocketAddress(), handshakeEntry.getDisplayName(),
                    protocol.getProtocol(), protocol.getMinecraftVersion());

            LoginData loginData = handshakeEntry.buildData(this.session, this.proxy);

            PlayerAuthenticatedEvent loginEvent = new PlayerAuthenticatedEvent(ProxiedPlayer.class, loginData, (InetSocketAddress) this.session.getSocketAddress());
            this.proxy.getEventManager().callEvent(loginEvent);
            if (loginEvent.isCancelled()) {
                this.session.disconnect(loginEvent.getCancelReason());
                return PacketSignal.HANDLED;
            }

            this.player = loginEvent.getBaseClass().getConstructor(ProxyServer.class, BedrockServerSession.class, CompressionType.class, LoginData.class)
                    .newInstance(this.proxy, this.session, this.compression, loginData);
            if (!this.proxy.getPlayerManager().registerPlayer(this.player)) {
                return PacketSignal.HANDLED;
            }

            if (this.proxy.getConfiguration().isUpstreamEncryption()) {
                HandshakeUtils.processEncryption(session, handshakeEntry.getIdentityPublicKey());
            } else {
                this.finishConnection();
            }
        } catch (Exception e) {
            this.onLoginFailed(handshakeEntry, e, "Login failed: " + e.getMessage());
            this.proxy.getLogger().error("[{}] Unable to complete login", this.session.getSocketAddress(), e);
        }
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(ClientToServerHandshakePacket packet) {
        this.finishConnection();
        return PacketSignal.HANDLED;
    }

    private void finishConnection() {
        PlayStatusPacket status = new PlayStatusPacket();
        status.setStatus(PlayStatusPacket.Status.LOGIN_SUCCESS);
        this.session.sendPacket(status);
        this.player.initPlayer();
    }
}
