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

package pe.waterdog.player;

import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.*;
import com.nukkitx.protocol.bedrock.BedrockSession;
import com.nukkitx.protocol.bedrock.packet.LoginPacket;
import com.nukkitx.protocol.bedrock.util.EncryptionUtils;
import pe.waterdog.ProxyServer;
import pe.waterdog.utils.ProxyConfig;

import java.net.URI;
import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;
import java.util.UUID;

/**
 * Various utilities for parsing Handshake data
 */
public class HandshakeUtils {

    public static boolean validateChain(JsonArray chainArray) throws Exception {
        ECPublicKey lastKey = null;
        boolean validChain = false;

        for (JsonElement element : chainArray){
            JWSObject jwt = JWSObject.parse(element.getAsString());
            if (!validChain) {
                validChain = EncryptionUtils.verifyJwt(jwt, EncryptionUtils.getMojangPublicKey());
            }

            if (lastKey != null) {
                EncryptionUtils.verifyJwt(jwt, lastKey);
            }

            JsonObject payload = (JsonObject) JsonParser.parseString(jwt.getPayload().toString());
            Preconditions.checkArgument(payload.has("identityPublicKey"), "IdentityPublicKey node is missing in chain!");
            JsonElement ipkNode = payload.get("identityPublicKey");
            lastKey = EncryptionUtils.generateKey(ipkNode.getAsString());
        }
        return validChain;
    }

    public static JWSObject createExtraData(KeyPair pair, JsonObject extraData) {
        String publicKeyBase64 = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
        long timestamp = System.currentTimeMillis() / 1000;

        JsonObject dataChain = new JsonObject();
        dataChain.addProperty("nbf", timestamp - 3600);
        dataChain.addProperty("exp", timestamp + 24 * 3600);
        dataChain.addProperty("iat", timestamp);
        dataChain.addProperty("iss", "self");
        dataChain.addProperty("certificateAuthority", true);
        dataChain.add("extraData", extraData);
        dataChain.addProperty("randomNonce", UUID.randomUUID().getLeastSignificantBits());
        dataChain.addProperty("identityPublicKey", publicKeyBase64);
        return encodeJWT(pair, dataChain);
    }

    public static JWSObject encodeJWT(KeyPair pair, JsonObject payload) {
        String publicKeyBase64 = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
        URI x5u = URI.create(publicKeyBase64);
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES384).x509CertURL(x5u).build();
        JWSObject jwsObject = new JWSObject(header, new Payload(payload.toString()));

        try {
            EncryptionUtils.signJwt(jwsObject, (ECPrivateKey) pair.getPrivate());
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
        return jwsObject;
    }

    public static JsonObject parseClientData(LoginPacket packet, JsonObject payload, BedrockSession session) throws Exception {
        String identityPublicKeyString = payload.get("identityPublicKey").getAsString();
        if (identityPublicKeyString == null) {
            throw new RuntimeException("Identity Public Key was not found!");
        }

        ECPublicKey identityPublicKey = EncryptionUtils.generateKey(identityPublicKeyString);
        JWSObject clientJwt = JWSObject.parse(packet.getSkinData().toString());
        EncryptionUtils.verifyJwt(clientJwt, identityPublicKey);

        JsonObject clientData = (JsonObject) JsonParser.parseString(clientJwt.getPayload().toString());

        /* Add WaterdogAttributes */
        ProxyConfig config = ProxyServer.getInstance().getConfiguration();
        if (config.useLoginExtras() && config.isIpForward()) {
            clientData.addProperty("Waterdog_IP", session.getAddress().getAddress().getHostAddress());
        }
        return clientData;
    }

    public static JsonObject parseExtraData(LoginPacket packet, JsonObject payload) {
        JsonElement extraDataElement = payload.get("extraData");
        if (!extraDataElement.isJsonObject()) {
            throw new IllegalStateException("Invalid 'extraData'");
        }

        JsonObject extraData = extraDataElement.getAsJsonObject();
        /* Replace spaces in name */
        if (ProxyServer.getInstance().getConfiguration().isReplaceUsernameSpaces()) {
            String playerName = extraData.get("displayName").getAsString();
            extraData.addProperty("displayName", playerName.replaceAll(" ", "_"));
        }
        return extraData;
    }
}
