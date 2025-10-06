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
import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.event.defaults.PreClientDataSetEvent;
import dev.waterdog.waterdogpe.network.connection.peer.BedrockServerSession;
import dev.waterdog.waterdogpe.network.protocol.ProtocolVersion;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.protocol.bedrock.util.EncryptionUtils;

import java.security.interfaces.ECPublicKey;
import java.util.UUID;

@Getter
public class HandshakeEntry {

    private final ECPublicKey identityPublicKey;
    private final JsonObject clientData;
    private final String xuid;
    private final UUID uuid;
    private final String displayName;
    private final boolean xboxAuthed;
    @Setter
    private ProtocolVersion protocol;
    private final boolean isChainPayload;

    public HandshakeEntry(ECPublicKey identityPublicKey, JsonObject clientData, String xuid, UUID uuid, String displayName, boolean xboxAuthed, ProtocolVersion protocol, boolean isChainPayload) {
        this.identityPublicKey = identityPublicKey;
        this.clientData = clientData;
        this.xuid = xuid;
        this.uuid = uuid;
        this.displayName = displayName;
        this.xboxAuthed = xboxAuthed;
        this.protocol = protocol;
        this.isChainPayload = isChainPayload;
    }

    public LoginData buildData(BedrockServerSession session, ProxyServer proxy) throws Exception {
        // This is first event which exposes new player connecting to proxy.
        // The purpose is to change player's client data or set encryption keypair before joining first downstream.
        PreClientDataSetEvent event = new PreClientDataSetEvent(this.clientData, this.xuid, this.uuid, this.displayName, EncryptionUtils.createKeyPair(), session);
        proxy.getEventManager().callEvent(event);

        LoginData.LoginDataBuilder builder = LoginData.builder();
        builder.displayName(this.displayName);
        builder.uuid(this.uuid);
        builder.xuid(this.xuid);
        builder.xboxAuthed(this.xboxAuthed);
        builder.protocol(this.protocol);
        builder.joinHostname(this.clientData.get("ServerAddress").getAsString().split(":")[0]);
        builder.address(session.getSocketAddress());
        builder.keyPair(event.getKeyPair());
        builder.clientData(this.clientData);
        builder.isChainPayload(this.isChainPayload);
        if (this.clientData.has("DeviceModel")) {
            builder.deviceModel(this.clientData.get("DeviceModel").getAsString());
        }
        if (this.clientData.has("DeviceOS")) {
            builder.devicePlatform(Platform.getPlatformByID(this.clientData.get("DeviceOS").getAsInt()));
        }
        if (this.clientData.has("DeviceId")) {
            builder.deviceId(this.clientData.get("DeviceId").getAsString());
        }
        return builder.build();
    }

}
