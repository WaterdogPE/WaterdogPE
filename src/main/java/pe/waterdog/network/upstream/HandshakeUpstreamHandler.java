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

import com.nimbusds.jose.JWSObject;
import com.nukkitx.protocol.bedrock.BedrockServerSession;
import com.nukkitx.protocol.bedrock.handler.BedrockPacketHandler;
import com.nukkitx.protocol.bedrock.packet.LoginPacket;
import com.nukkitx.protocol.bedrock.packet.PlayStatusPacket;
import com.nukkitx.protocol.bedrock.util.EncryptionUtils;
import io.netty.util.AsciiString;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONStyle;
import net.minidev.json.JSONValue;
import pe.waterdog.ProxyServer;
import pe.waterdog.VersionInfo;
import pe.waterdog.event.defaults.PlayerCreationEvent;
import pe.waterdog.event.defaults.PlayerPreLoginEvent;
import pe.waterdog.network.protocol.ProtocolConstants;
import pe.waterdog.network.session.LoginData;
import pe.waterdog.player.HandshakeUtils;
import pe.waterdog.player.ProxiedPlayer;

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
        this.proxy.getLogger().info("[" + session.getAddress() + "] <-> Upstream has connected (protocol="+protocolVersion+")");

        ProtocolConstants.Protocol protocol = ProtocolConstants.get(protocolVersion);
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

        JSONObject certData = (JSONObject) JSONValue.parse(packet.getChainData().toByteArray());
        Object chainObject = certData.get("chain");

        if (!(chainObject instanceof JSONArray)) throw new RuntimeException("Certificate data is not valid");
        JSONArray certChain = (JSONArray) chainObject;

        try {
            EncryptionUtils.verifyChain(certChain);
            JWSObject jwt = JWSObject.parse((String) certChain.get(certChain.size() - 1));
            JSONObject payload = jwt.getPayload().toJSONObject();

            JSONObject clientData = HandshakeUtils.parseClientData(packet, payload, session);
            JSONObject extraData = HandshakeUtils.parseExtraData(packet, payload);
            KeyPair keyPair = EncryptionUtils.createKeyPair();

            LoginData loginData = new LoginData(
                    extraData.getAsString("displayName"),
                    UUID.fromString(extraData.getAsString("identity")),
                    extraData.getAsString("XUID"),
                    extraData.containsKey("XUID"), //XBOX auth
                    protocol,
                    clientData.getAsString("ServerAddress").split(":")[0],
                    this.session.getAddress(),
                    keyPair
            );

            PlayerPreLoginEvent event = new PlayerPreLoginEvent(loginData);
            this.proxy.getEventManager().callEvent(event);
            if (event.isCancelled()) {
                // Pre Login was cancelled
                session.disconnect(event.getCancelReason());
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

            player.initialConnect();
        } catch (Exception e) {
            session.disconnect("disconnectionScreen.internalError.cantConnect");
            throw new RuntimeException("Unable to complete login", e);
        }
        return true;
    }

}
