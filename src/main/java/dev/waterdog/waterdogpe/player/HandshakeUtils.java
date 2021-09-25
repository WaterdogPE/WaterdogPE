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

import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.*;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jwt.SignedJWT;
import com.nukkitx.protocol.bedrock.BedrockSession;
import com.nukkitx.protocol.bedrock.packet.LoginPacket;
import com.nukkitx.protocol.bedrock.packet.ServerToClientHandshakePacket;
import com.nukkitx.protocol.bedrock.util.EncryptionUtils;
import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.network.protocol.ProtocolVersion;
import dev.waterdog.waterdogpe.utils.config.ProxyConfig;

import javax.crypto.SecretKey;
import java.net.URI;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;
import java.util.Iterator;
import java.util.UUID;

/**
 * Various utilities for parsing Handshake data
 */
public class HandshakeUtils {

    private static final KeyPair privateKeyPair;

    static {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(Curve.P_384.toECParameterSpec());
            privateKeyPair = generator.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("Unable to generate private keyPair!", e);
        }
    }

    public static KeyPair getPrivateKeyPair() {
        return privateKeyPair;
    }

    public static boolean validateChain(JsonArray chainArray, boolean strict) throws Exception {
        if (strict && chainArray.size() > 3) {
            // We dont expect larger chain
            return false;
        }

        ECPublicKey lastKey = null;
        boolean authed = false;
        Iterator<JsonElement> iterator = chainArray.iterator();
        while(iterator.hasNext()){
            JsonElement element = iterator.next();
            SignedJWT jwt = SignedJWT.parse(element.getAsString());

            URI x5u = jwt.getHeader().getX509CertURL();
            if (x5u == null) {
                throw new JOSEException("Key not found");
            }

            ECPublicKey expectedKey = EncryptionUtils.generateKey(jwt.getHeader().getX509CertURL().toString());
            if (lastKey == null) {
                // First key is self signed
                lastKey = expectedKey;
            } else if (strict && !lastKey.equals(expectedKey)) {
                // Make sure the previous key matches the header of the current
                throw new IllegalArgumentException("Key does not match");
            }

            if (!EncryptionUtils.verifyJwt(jwt, lastKey)) {
                if (strict) {
                    throw new JOSEException("Login JWT was not valid");
                }
                return false;
            }

            if (lastKey.equals(EncryptionUtils.getMojangPublicKey())) {
                authed = true;
            } else if (authed) {
                return !iterator.hasNext();
            }

            JsonObject payload = (JsonObject) JsonParser.parseString(jwt.getPayload().toString());
            Preconditions.checkArgument(payload.has("identityPublicKey"), "IdentityPublicKey node is missing in chain!");
            JsonElement ipkNode = payload.get("identityPublicKey");
            lastKey = EncryptionUtils.generateKey(ipkNode.getAsString());
        }
        return authed;
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

    public static HandshakeEntry processHandshake(BedrockSession session, LoginPacket packet, JsonArray certChain, ProtocolVersion protocol, boolean strict) throws Exception {
        // Cert chain should be signed by Mojang is is client xbox authenticated
        boolean xboxAuth = HandshakeUtils.validateChain(certChain, strict);
        JWSObject jwt = JWSObject.parse(certChain.get(certChain.size() - 1).getAsString());
        JsonObject payload = (JsonObject) JsonParser.parseString(jwt.getPayload().toString());
        JsonObject extraData = HandshakeUtils.parseExtraData(packet, payload);

        if (!payload.has("identityPublicKey")) {
            throw new RuntimeException("Identity Public Key was not found!");
        }
        String identityPublicKeyString = payload.get("identityPublicKey").getAsString();

        ECPublicKey identityPublicKey = EncryptionUtils.generateKey(identityPublicKeyString);
        JWSObject clientJwt = JWSObject.parse(packet.getSkinData().toString());
        if (!EncryptionUtils.verifyJwt(clientJwt, identityPublicKey) && strict) {
            xboxAuth = false;
        }
        JsonObject clientData = HandshakeUtils.parseClientData(clientJwt, extraData, session);
        return new HandshakeEntry(identityPublicKey, clientData, extraData, xboxAuth, protocol);
    }

    public static JsonObject parseClientData(JWSObject clientJwt, JsonObject extraData, BedrockSession session) throws Exception {
        JsonObject clientData = (JsonObject) JsonParser.parseString(clientJwt.getPayload().toString());
        ProxyConfig config = ProxyServer.getInstance().getConfiguration();
        if (config.useLoginExtras()) {
            // Add waterdog attributes
            clientData.addProperty("Waterdog_XUID", extraData.get("XUID").getAsString());
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
        if (ProxyServer.getInstance().getConfiguration().isReplaceUsernameSpaces()) {
            String playerName = extraData.get("displayName").getAsString();
            extraData.addProperty("displayName", playerName.replaceAll(" ", "_"));
        }
        return extraData;
    }

    public static void processEncryption(BedrockSession session, PublicKey key) throws Exception {
        byte[] token = EncryptionUtils.generateRandomToken();
        SecretKey encryptionKey = EncryptionUtils.getSecretKey(privateKeyPair.getPrivate(), key, token);
        session.enableEncryption(encryptionKey);

        ServerToClientHandshakePacket packet = new ServerToClientHandshakePacket();
        packet.setJwt(EncryptionUtils.createHandshakeJwt(privateKeyPair, token).serialize());
        session.sendPacketImmediately(packet);
    }
}
