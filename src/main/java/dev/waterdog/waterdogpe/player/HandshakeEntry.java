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

package dev.waterdog.waterdogpe.player;

import com.google.gson.JsonObject;
import com.nukkitx.protocol.bedrock.BedrockServerSession;
import com.nukkitx.protocol.bedrock.util.EncryptionUtils;
import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.event.defaults.PreClientDataSetEvent;
import dev.waterdog.waterdogpe.network.protocol.ProtocolVersion;
import dev.waterdog.waterdogpe.network.session.LoginData;

import java.security.interfaces.ECPublicKey;
import java.util.UUID;

public class HandshakeEntry {

    private final ECPublicKey identityPublicKey;
    private final JsonObject clientData;
    private final JsonObject extraData;
    private final boolean xboxAuthed;
    private ProtocolVersion protocol;

    public HandshakeEntry(ECPublicKey identityPublicKey, JsonObject clientData, JsonObject extraData, boolean xboxAuthed, ProtocolVersion protocol) {
        this.identityPublicKey = identityPublicKey;
        this.clientData = clientData;
        this.extraData = extraData;
        this.xboxAuthed = xboxAuthed;
        this.protocol = protocol;
    }

    public LoginData buildData(BedrockServerSession session, ProxyServer proxy) throws Exception {
        // This is first event which exposes new player connecting to proxy.
        // The purpose is to change player's client data or set encryption keypair before joining first downstream.
        PreClientDataSetEvent event = new PreClientDataSetEvent(this.clientData, this.extraData, EncryptionUtils.createKeyPair(), session);
        proxy.getEventManager().callEvent(event);

        LoginData.LoginDataBuilder builder = LoginData.builder();
        builder.displayName(this.extraData.get("displayName").getAsString());
        builder.uuid(UUID.fromString(this.extraData.get("identity").getAsString()));
        builder.xuid(this.extraData.get("XUID").getAsString());
        builder.xboxAuthed(this.xboxAuthed);
        // Thank you Mojang: this version includes protocol changes, but protocol version was not increased.
        var gameVersion = getClientData().has("GameVersion") ? getClientData().get("GameVersion").getAsString().replaceAll("\n", "") : null;
        if (protocol.equals(ProtocolVersion.MINECRAFT_PE_1_19_60) && ProtocolVersion.MINECRAFT_PE_1_19_62.getMinecraftVersion().equals(gameVersion)) {
            setProtocol(ProtocolVersion.MINECRAFT_PE_1_19_62);
        }
        builder.protocol(this.protocol);
        builder.joinHostname(this.clientData.get("ServerAddress").getAsString().split(":")[0]);
        builder.address(session.getAddress());
        builder.keyPair(event.getKeyPair());
        builder.clientData(this.clientData);
        builder.extraData(this.extraData);
        if (this.clientData.has("DeviceModel")) {
            builder.deviceModel(this.clientData.get("DeviceModel").getAsString());
        }
        if (this.clientData.has("DeviceOS")) {
            builder.devicePlatform(Platform.getPlatformByID(this.clientData.get("DeviceOS").getAsInt()));
        }
        if (this.clientData.has("DeviceId")) {
            builder.deviceId(this.clientData.get("DeviceId").getAsString());
        }


        if (proxy.getConfiguration().isUpstreamEncryption()) {
            HandshakeUtils.processEncryption(session, this.identityPublicKey);
        }
        return builder.build();
    }

    public ECPublicKey getIdentityPublicKey() {
        return this.identityPublicKey;
    }

    public boolean isXboxAuthed() {
        return this.xboxAuthed;
    }

    public String getDisplayName() {
        return this.extraData.get("displayName").getAsString();
    }

    public void setProtocol(ProtocolVersion protocol) {
        this.protocol = protocol;
    }

    public ProtocolVersion getProtocol() {
        return this.protocol;
    }

    public JsonObject getClientData() {
        return this.clientData;
    }

    public JsonObject getExtraData() {
        return this.extraData;
    }
}
