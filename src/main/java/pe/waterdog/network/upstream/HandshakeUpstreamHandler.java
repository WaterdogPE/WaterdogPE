/**
 * Copyright 2020 WaterdogTEAM
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pe.waterdog.network.upstream;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JWSObject;
import com.nukkitx.protocol.bedrock.BedrockServerSession;
import com.nukkitx.protocol.bedrock.handler.BedrockPacketHandler;
import com.nukkitx.protocol.bedrock.packet.LoginPacket;
import com.nukkitx.protocol.bedrock.packet.PlayStatusPacket;
import com.nukkitx.protocol.bedrock.util.EncryptionUtils;
import io.netty.util.AsciiString;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONStyle;
import pe.waterdog.ProxyServer;
import pe.waterdog.VersionInfo;
import pe.waterdog.event.defaults.PlayerCreationEvent;
import pe.waterdog.event.defaults.PlayerPreLoginEvent;
import pe.waterdog.event.defaults.PreClientDataSetEvent;
import pe.waterdog.network.protocol.ProtocolConstants;
import pe.waterdog.network.protocol.ProtocolVersion;
import pe.waterdog.network.session.LoginData;
import pe.waterdog.player.HandshakeUtils;
import pe.waterdog.player.ProxiedPlayer;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.security.KeyPair;
import java.util.Collections;
import java.util.UUID;

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
        this.proxy.getLogger().info("[" + session.getAddress() + "] <-> Upstream has connected (protocol=" + protocolVersion + ")");

        ProtocolVersion protocol = ProtocolConstants.get(protocolVersion);
        session.setPacketCodec(protocol == null ? ProtocolConstants.getLatestProtocol().getCodec() : protocol.getCodec());
        if (protocolVersion != VersionInfo.LATEST_PROTOCOL_VERSION && protocol == null) {
            PlayStatusPacket status = new PlayStatusPacket();
            status.setStatus((protocolVersion > VersionInfo.LATEST_PROTOCOL_VERSION ?
                    PlayStatusPacket.Status.LOGIN_FAILED_SERVER_OLD :
                    PlayStatusPacket.Status.LOGIN_FAILED_CLIENT_OLD));

            session.sendPacket(status);
            return true;
        }

        session.setLogging(true);

        JsonObject certJson = (JsonObject) JsonParser.parseReader(new InputStreamReader(new ByteArrayInputStream(packet.getChainData().toByteArray())));
        if (!certJson.has("chain") || !certJson.getAsJsonObject().get("chain").isJsonArray()){
            throw new RuntimeException("Certificate data is not valid");
        }
        JsonArray certChain = certJson.getAsJsonArray("chain");

        try {
            // Cert chain should be signed by Mojang is is client xbox authenticated
            boolean xboxAuth = HandshakeUtils.validateChain(certChain);
            JWSObject jwt = JWSObject.parse(certChain.get(certChain.size() - 1).getAsString());
            JsonObject payload = (JsonObject) JsonParser.parseString(jwt.getPayload().toString());

            JsonObject clientData = HandshakeUtils.parseClientData(packet, payload, session);
            JsonObject extraData = HandshakeUtils.parseExtraData(packet, payload);
            KeyPair keyPair = EncryptionUtils.createKeyPair();

            PreClientDataSetEvent event = new PreClientDataSetEvent(clientData, extraData, this.session);
            this.proxy.getEventManager().callEvent(event);

            LoginData loginData = new LoginData(
                    extraData.get("displayName").getAsString(),
                    UUID.fromString(extraData.get("identity").getAsString()),
                    extraData.get("XUID").getAsString(),
                    xboxAuth,
                    protocol,
                    clientData.get("ServerAddress").getAsString().split(":")[0],
                    this.session.getAddress(),
                    keyPair
            );
            
            if (!loginData.isXboxAuthed() && this.proxy.getConfiguration().isOnlineMode()) {
                this.proxy.getLogger().info("[" + session.getAddress() + "] <-> Upstream has disconnected due to failed XBOX authentication!");
                session.disconnect("disconnectionScreen.notAuthenticated");
                return false;
            }

            PlayerPreLoginEvent preLoginEvent = new PlayerPreLoginEvent(loginData);
            this.proxy.getEventManager().callEvent(preLoginEvent);
            if (preLoginEvent.isCancelled()) {
                session.disconnect(preLoginEvent.getCancelReason());
                return true;
            }

            JWSObject signedExtraData = HandshakeUtils.createExtraData(keyPair, extraData);
            JWSObject signedClientData = HandshakeUtils.encodeJWT(keyPair, clientData);

            JSONObject chainJson = new JSONObject();
            chainJson.put("chain", Collections.singletonList(signedExtraData.serialize()));
            AsciiString chainData = AsciiString.of(chainJson.toString(JSONStyle.LT_COMPRESS));

            loginData.setChainData(chainData);
            loginData.setSignedClientData(signedClientData);

            PlayerCreationEvent creationEvent = new PlayerCreationEvent(ProxiedPlayer.class, loginData, this.session.getAddress());
            this.proxy.getEventManager().callEvent(creationEvent);

            ProxiedPlayer player = creationEvent.getBaseClass().getConstructor(ProxyServer.class, BedrockServerSession.class, LoginData.class).newInstance(this.proxy, this.session, loginData);
            if (!this.proxy.getPlayerManager().registerPlayer(player)) {
                return true;
            }

            PlayStatusPacket status = new PlayStatusPacket();
            status.setStatus(PlayStatusPacket.Status.LOGIN_SUCCESS);
            this.session.sendPacket(status);

            player.initPlayer();
        } catch (Exception e) {
            session.disconnect("disconnectionScreen.internalError.cantConnect");
            throw new RuntimeException("Unable to complete login", e);
        }
        return true;
    }

}
