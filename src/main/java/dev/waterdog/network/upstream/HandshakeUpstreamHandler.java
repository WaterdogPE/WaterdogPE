/*
 * Copyright 2021 WaterdogTEAM
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

package dev.waterdog.network.upstream;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nukkitx.protocol.bedrock.BedrockServerSession;
import com.nukkitx.protocol.bedrock.handler.BedrockPacketHandler;
import com.nukkitx.protocol.bedrock.packet.LoginPacket;
import com.nukkitx.protocol.bedrock.packet.PlayStatusPacket;
import dev.waterdog.ProxyServer;
import dev.waterdog.VersionInfo;
import dev.waterdog.event.defaults.PlayerPreLoginEvent;
import dev.waterdog.network.protocol.ProtocolConstants;
import dev.waterdog.network.protocol.ProtocolVersion;
import dev.waterdog.network.session.LoginData;
import dev.waterdog.player.HandshakeEntry;
import dev.waterdog.player.HandshakeUtils;
import dev.waterdog.player.ProxiedPlayer;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;

/**
 * The Pipeline Handler handling the login handshake part of the initial connect. Will be replaced after success.
 */
public class HandshakeUpstreamHandler implements BedrockPacketHandler {

    private final ProxyServer proxy;
    private final BedrockServerSession session;

    public HandshakeUpstreamHandler(ProxyServer proxy, BedrockServerSession session) {
        this.proxy = proxy;
        this.session = session;
    }

    @Override
    public boolean handle(LoginPacket packet) {
        int protocolVersion = packet.getProtocolVersion();
        ProtocolVersion protocol = ProtocolConstants.get(protocolVersion);
        session.setPacketCodec(protocol == null ? ProtocolConstants.getLatestProtocol().getCodec() : protocol.getCodec());

        if (protocolVersion != VersionInfo.LATEST_PROTOCOL_VERSION && protocol == null) {
            this.proxy.getLogger().alert("[" + session.getAddress() + "] <-> Upstream has disconnected due to incompatible protocol (protocol=" + protocolVersion + ")");
            PlayStatusPacket status = new PlayStatusPacket();
            status.setStatus((protocolVersion > VersionInfo.LATEST_PROTOCOL_VERSION ?
                    PlayStatusPacket.Status.LOGIN_FAILED_SERVER_OLD :
                    PlayStatusPacket.Status.LOGIN_FAILED_CLIENT_OLD));

            session.sendPacketImmediately(status);
            session.disconnect();
            return true;
        }

        session.setLogging(true);

        JsonObject certJson = (JsonObject) JsonParser.parseReader(new InputStreamReader(new ByteArrayInputStream(packet.getChainData().toByteArray())));
        if (!certJson.has("chain") || !certJson.getAsJsonObject().get("chain").isJsonArray()) {
            throw new RuntimeException("Certificate data is not valid");
        }
        JsonArray certChain = certJson.getAsJsonArray("chain");

        try {
            HandshakeEntry handshakeEntry = HandshakeUtils.processHandshake(session, packet, certChain, protocol);
            if (!handshakeEntry.isXboxAuthed() && this.proxy.getConfiguration().isOnlineMode()) {
                this.proxy.getLogger().info("[" + session.getAddress() + "|" + handshakeEntry.getDisplayName() + "] <-> Upstream has disconnected due to failed XBOX authentication!");
                session.disconnect("disconnectionScreen.notAuthenticated");
                return true;
            }

            this.proxy.getLogger().info("[" + session.getAddress() + "|" + handshakeEntry.getDisplayName() + "] <-> Upstream has connected (protocol=" + protocolVersion + ")");
            LoginData loginData = handshakeEntry.buildData(session, this.proxy);

            PlayerPreLoginEvent loginEvent = new PlayerPreLoginEvent(ProxiedPlayer.class, loginData, this.session.getAddress());
            this.proxy.getEventManager().callEvent(loginEvent);
            if (loginEvent.isCancelled()) {
                session.disconnect(loginEvent.getCancelReason());
                return true;
            }

            ProxiedPlayer player = loginEvent.getBaseClass().getConstructor(ProxyServer.class, BedrockServerSession.class, LoginData.class).newInstance(this.proxy, this.session, loginData);
            if (!this.proxy.getPlayerManager().registerPlayer(player)) {
                return true;
            }

            PlayStatusPacket status = new PlayStatusPacket();
            status.setStatus(PlayStatusPacket.Status.LOGIN_SUCCESS);
            this.session.sendPacket(status);

            player.initPlayer();
        } catch (Exception e) {
            session.disconnect("Login failed: "+e.getMessage());
            this.proxy.getLogger().error("[" + session.getAddress() + "] Unable to complete login", e);
        }
        return true;
    }

}
