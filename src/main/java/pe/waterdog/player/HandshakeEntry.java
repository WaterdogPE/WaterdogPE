/*
 * Copyright 2021 WaterdogTEAM
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package pe.waterdog.player;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JWSObject;
import com.nukkitx.protocol.bedrock.BedrockServerSession;
import com.nukkitx.protocol.bedrock.util.EncryptionUtils;
import io.netty.util.AsciiString;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONStyle;
import pe.waterdog.ProxyServer;
import pe.waterdog.event.defaults.PreClientDataSetEvent;
import pe.waterdog.network.protocol.ProtocolVersion;
import pe.waterdog.network.session.LoginData;

import java.security.KeyPair;
import java.util.Collections;
import java.util.UUID;

public class HandshakeEntry {

    private final JsonObject clientData;
    private final JsonObject extraData;
    private final boolean xboxAuthed;
    private final ProtocolVersion protocol;

    public HandshakeEntry(JsonObject clientData, JsonObject extraData, boolean xboxAuthed, ProtocolVersion protocol) {
        this.clientData = clientData;
        this.extraData = extraData;
        this.xboxAuthed = xboxAuthed;
        this.protocol = protocol;
    }

    public LoginData buildData(BedrockServerSession session, ProxyServer proxy) {
        // This is first event which exposes new player connecting to proxy.
        // The purpose is to change player's client data before joining first downstream.
        PreClientDataSetEvent event = new PreClientDataSetEvent(clientData, extraData, session);
        proxy.getEventManager().callEvent(event);

        LoginData.LoginDataBuilder builder = LoginData.builder();
        builder.displayName(extraData.get("displayName").getAsString());
        builder.uuid(UUID.fromString(extraData.get("identity").getAsString()));
        builder.xuid(extraData.get("XUID").getAsString());
        builder.xboxAuthed(this.xboxAuthed);
        builder.protocol(this.protocol);
        builder.joinHostname(clientData.get("ServerAddress").getAsString().split(":")[0]);
        builder.address(session.getAddress());

        KeyPair keyPair = EncryptionUtils.createKeyPair();
        builder.keyPair(keyPair);

        JWSObject signedClientData = HandshakeUtils.encodeJWT(keyPair, clientData);
        JWSObject signedExtraData = HandshakeUtils.createExtraData(keyPair, extraData);

        JSONObject chainJson = new JSONObject();
        chainJson.put("chain", Collections.singletonList(signedExtraData.serialize()));
        AsciiString chainData = AsciiString.of(chainJson.toString(JSONStyle.LT_COMPRESS));

        builder.signedClientData(signedClientData);
        builder.chainData(chainData);
        return builder.build();
    }

    public boolean isXboxAuthed() {
        return this.xboxAuthed;
    }

    public String getDisplayName() {
        return this.extraData.get("displayName").getAsString();
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
