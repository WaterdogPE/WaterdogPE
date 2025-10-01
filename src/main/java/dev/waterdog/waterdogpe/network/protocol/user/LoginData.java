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

package dev.waterdog.waterdogpe.network.protocol.user;

import com.google.gson.JsonObject;
import com.nimbusds.jwt.SignedJWT;
import dev.waterdog.waterdogpe.network.protocol.ProtocolVersion;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.cloudburstmc.protocol.bedrock.data.auth.AuthType;
import org.cloudburstmc.protocol.bedrock.data.auth.CertificateChainPayload;
import org.cloudburstmc.protocol.bedrock.data.auth.TokenPayload;
import org.cloudburstmc.protocol.bedrock.packet.ClientCacheStatusPacket;
import org.cloudburstmc.protocol.bedrock.packet.LoginPacket;
import org.cloudburstmc.protocol.bedrock.packet.RequestChunkRadiusPacket;

import java.net.SocketAddress;
import java.security.KeyPair;
import java.util.Collections;
import java.util.UUID;

/**
 * Holds relevant information passed to the proxy on the first connection (initial) in the LoginPacket.
 */
@Slf4j
@Builder
@Getter
public class LoginData {

    private final String displayName;
    private final UUID uuid;
    private final String xuid;
    private final boolean xboxAuthed;
    private final SocketAddress address;
    private final ProtocolVersion protocol;
    private final String joinHostname;

    @Builder.Default
    private final Platform devicePlatform = Platform.UNKNOWN;
    @Builder.Default
    private final String deviceModel = null;
    @Builder.Default
    private final String deviceId = null;

    private final KeyPair keyPair;
    private final JsonObject clientData;
    private LoginPacket loginPacket;

    @Setter
    @Builder.Default
    private RequestChunkRadiusPacket chunkRadius = PlayerRewriteUtils.defaultChunkRadius;
    @Setter
    @Builder.Default
    private ClientCacheStatusPacket cachePacket = PlayerRewriteUtils.defaultCachePacket;

    private final boolean isChainPayload;

    /**
     * Used to construct new login packet using this.clientData and this.extraData signed by this.keyPair.
     * This method should be called everytime client data is changed. Otherwise player will join to downstream using old data.
     *
     * @return new LoginPacket.
     */
    public LoginPacket rebuildLoginPacket() {
        LoginPacket loginPacket = new LoginPacket();
        SignedJWT signedClientData = HandshakeUtils.encodeJWT(this.keyPair, this.clientData);
        loginPacket.setClientJwt(signedClientData.serialize());
        loginPacket.setProtocolVersion(this.protocol.getProtocol());
        if (isChainPayload) {
            JsonObject extraData = HandshakeUtils.createChainExtraData(displayName, xuid, uuid);
            SignedJWT signedPayload = HandshakeUtils.createClientDataChain(this.keyPair, extraData);
            loginPacket.setAuthPayload(new CertificateChainPayload(Collections.singletonList(signedPayload.serialize()), AuthType.SELF_SIGNED));
        } else {
            SignedJWT signedPayload = HandshakeUtils.createClientDataToken(this.keyPair, displayName, xuid);
            loginPacket.setAuthPayload(new TokenPayload(signedPayload.serialize(), AuthType.SELF_SIGNED));
        }
        this.loginPacket = loginPacket;
        return loginPacket;
    }

    public LoginPacket getLoginPacket() {
        if (this.loginPacket == null) {
            this.rebuildLoginPacket();
        }
        return this.loginPacket;
    }

}
