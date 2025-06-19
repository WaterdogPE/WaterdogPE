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
import org.cloudburstmc.protocol.bedrock.data.auth.AuthType;
import org.cloudburstmc.protocol.bedrock.data.auth.CertificateChainPayload;
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
@Builder
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
    @Deprecated
    private final JsonObject extraData;

    private LoginPacket loginPacket;

    @Builder.Default
    private RequestChunkRadiusPacket chunkRadius = PlayerRewriteUtils.defaultChunkRadius;
    @Builder.Default
    private ClientCacheStatusPacket cachePacket = PlayerRewriteUtils.defaultCachePacket;

    /**
     * Used to construct new login packet using this.clientData and this.extraData signed by this.keyPair.
     * This method should be called everytime client data is changed. Otherwise player will join to downstream using old data.
     *
     * @return new LoginPacket.
     */
    public LoginPacket rebuildLoginPacket() {
        SignedJWT signedClientData = HandshakeUtils.createExtraData(this.keyPair, this.extraData);
        SignedJWT signedExtraData = HandshakeUtils.encodeJWT(this.keyPair, this.clientData);

        LoginPacket loginPacket = new LoginPacket();
        // Even if upstream sent TokenPayload, we use the CertificateChainPayload for compatability
        loginPacket.setAuthPayload(new CertificateChainPayload(Collections.singletonList(signedClientData.serialize()), AuthType.SELF_SIGNED));
        loginPacket.setClientJwt(signedExtraData.serialize());
        loginPacket.setProtocolVersion(this.protocol.getProtocol());
        return this.loginPacket = loginPacket;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public String getXuid() {
        return this.xuid;
    }

    public boolean isXboxAuthed() {
        return this.xboxAuthed;
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public SocketAddress getAddress() {
        return this.address;
    }

    public ProtocolVersion getProtocol() {
        return this.protocol;
    }

    public KeyPair getKeyPair() {
        return this.keyPair;
    }

    public JsonObject getClientData() {
        return this.clientData;
    }

    @Deprecated
    public JsonObject getExtraData() {
        return this.extraData;
    }

    public String getJoinHostname() {
        return this.joinHostname;
    }

    public Platform getDevicePlatform() {
        return devicePlatform;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getDeviceModel() {
        return deviceModel;
    }

    public LoginPacket getLoginPacket() {
        if (this.loginPacket == null) {
            this.rebuildLoginPacket();
        }
        return this.loginPacket;
    }

    public RequestChunkRadiusPacket getChunkRadius() {
        return this.chunkRadius;
    }

    public void setChunkRadius(RequestChunkRadiusPacket chunkRadius) {
        this.chunkRadius = chunkRadius;
    }

    public ClientCacheStatusPacket getCachePacket() {
        return this.cachePacket;
    }

    public void setCachePacket(ClientCacheStatusPacket cachePacket) {
        this.cachePacket = cachePacket;
    }
}
