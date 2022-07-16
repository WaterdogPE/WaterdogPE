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

package dev.waterdog.waterdogpe.network.session;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.shaded.json.JSONObject;
import com.nimbusds.jose.shaded.json.JSONStyle;
import com.nukkitx.protocol.bedrock.packet.ClientCacheStatusPacket;
import com.nukkitx.protocol.bedrock.packet.LoginPacket;
import com.nukkitx.protocol.bedrock.packet.RequestChunkRadiusPacket;
import dev.waterdog.waterdogpe.network.protocol.ProtocolVersion;
import dev.waterdog.waterdogpe.player.HandshakeUtils;
import dev.waterdog.waterdogpe.player.Platform;
import dev.waterdog.waterdogpe.player.PlayerRewriteUtils;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import io.netty.util.AsciiString;
import lombok.Builder;

import java.net.InetSocketAddress;
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
    private final InetSocketAddress address;
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
        JWSObject signedClientData = HandshakeUtils.encodeJWT(this.keyPair, this.clientData);
        JWSObject signedExtraData = HandshakeUtils.createExtraData(this.keyPair, this.extraData);

        JSONObject chainJson = new JSONObject();
        chainJson.put("chain", Collections.singletonList(signedExtraData.serialize()));
        AsciiString chainData = AsciiString.of(chainJson.toString(JSONStyle.LT_COMPRESS));

        LoginPacket loginPacket = new LoginPacket();
        loginPacket.setChainData(chainData);
        loginPacket.setSkinData(AsciiString.of(signedClientData.serialize()));
        loginPacket.setProtocolVersion(this.protocol.getProtocol());
        return this.loginPacket = loginPacket;
    }

    public void doLogin(DownstreamSession session) {
        session.sendPacketImmediately(this.getLoginPacket());
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

    public InetSocketAddress getAddress() {
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
