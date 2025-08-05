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

import com.google.gson.*;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.network.protocol.ProtocolVersion;
import dev.waterdog.waterdogpe.utils.config.proxy.ProxyConfig;
import org.cloudburstmc.protocol.bedrock.BedrockSession;
import org.cloudburstmc.protocol.bedrock.data.auth.CertificateChainPayload;
import org.cloudburstmc.protocol.bedrock.data.auth.TokenPayload;
import org.cloudburstmc.protocol.bedrock.packet.LoginPacket;
import org.cloudburstmc.protocol.bedrock.packet.ServerToClientHandshakePacket;
import org.cloudburstmc.protocol.bedrock.util.ChainValidationResult;
import org.cloudburstmc.protocol.bedrock.util.EncryptionUtils;

import javax.crypto.SecretKey;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.text.ParseException;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * Various utilities for parsing Handshake data
 */
public class HandshakeUtils {

    private static final KeyPair privateKeyPair;

    private static final Gson GSON = new GsonBuilder().create();

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

    public static SignedJWT createExtraData(KeyPair pair, JsonObject extraData) {
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

    public static SignedJWT encodeJWT(KeyPair pair, JsonObject payload) {
        String publicKeyBase64 = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
        URI x5u = URI.create(publicKeyBase64);
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES384).x509CertURL(x5u).build();
        try {
            SignedJWT jwt = new SignedJWT(header, JWTClaimsSet.parse(payload.toString()));
            signJwt(jwt, (ECPrivateKey) pair.getPrivate());
            return jwt;
        } catch (JOSEException | ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public static ECPublicKey generateKey(String b64) throws NoSuchAlgorithmException, InvalidKeySpecException {
        return (ECPublicKey) KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(b64)));
    }

    public static void signJwt(JWSObject jws, ECPrivateKey key) throws JOSEException {
        jws.sign(new ECDSASigner(key, Curve.P_384));
    }

    public static boolean verifyJwt(JWSObject jws, ECPublicKey key) throws JOSEException {
        return jws.verify(new ECDSAVerifier(key));
    }

    public static HandshakeEntry processHandshake(BedrockSession session, LoginPacket packet, ProtocolVersion protocol, boolean strict) throws Exception {
        if (packet.getAuthPayload() instanceof TokenPayload) {
            ChainValidationResult result = EncryptionUtils.validatePayload(packet.getAuthPayload());
            boolean xboxAuth = result.signed();
            JsonObject payload = GSON.toJsonTree(result.rawIdentityClaims()).getAsJsonObject();
            SignedJWT clientDataJwt = SignedJWT.parse(packet.getClientJwt());
            // Currently, we are still sending the legacy Certificate to downstream
            // For this, we need to build the extraData manually

            // xname is the old displayName field
            // xid is the old XUID field
            // identity field is removed, we derive that information from xuid instead

            // titleId and sandboxId are missing now, not sure if there is a way to derive those.

            JsonObject extraData = new JsonObject();
            String playerName = payload.get("xname").getAsString();
            if (ProxyServer.getInstance().getConfiguration().isReplaceUsernameSpaces()) {
                extraData.addProperty("displayName", playerName.replaceAll(" ", "_"));
            } else {
                extraData.addProperty("displayName", playerName);
            }
            String xuid = payload.get("xid").getAsString();
            extraData.addProperty("XUID", xuid);
            extraData.addProperty("identity", UUID.nameUUIDFromBytes(xuid.getBytes(StandardCharsets.UTF_8)).toString());
            ECPublicKey identityPublicKey = EncryptionUtils.parseKey(result.identityClaims().identityPublicKey);
            JsonObject clientData = HandshakeUtils.parseClientData(clientDataJwt, extraData, session);
            SignedJWT clientJwt = SignedJWT.parse(packet.getClientJwt());
            if (!verifyJwt(clientJwt, identityPublicKey) && strict) {
                xboxAuth = false;
            }
            return new HandshakeEntry(identityPublicKey, clientData, extraData, xboxAuth, protocol);
        } else if (packet.getAuthPayload() instanceof CertificateChainPayload chainPayload) {
            List<String> chain = chainPayload.getChain();
            if (chain.isEmpty()) {
                throw new IllegalArgumentException("Invalid chain data");
            }

            ChainValidationResult result = EncryptionUtils.validatePayload(packet.getAuthPayload());
            boolean xboxAuth = result.signed();
            JsonObject payload = GSON.toJsonTree(result.rawIdentityClaims()).getAsJsonObject();
            JsonObject extraData = HandshakeUtils.parseExtraData(payload);

            if (!payload.has("identityPublicKey")) {
                throw new RuntimeException("Identity Public Key was not found!");
            }
            String identityPublicKeyString = payload.get("identityPublicKey").getAsString();

            ECPublicKey identityPublicKey = generateKey(identityPublicKeyString);
            SignedJWT clientJwt = SignedJWT.parse(packet.getClientJwt());
            if (!verifyJwt(clientJwt, identityPublicKey) && strict) {
                xboxAuth = false;
            }
            JsonObject clientData = HandshakeUtils.parseClientData(clientJwt, extraData, session);

            return new HandshakeEntry(identityPublicKey, clientData, extraData, xboxAuth, protocol);
        } else {
            throw new IllegalArgumentException("Invalid auth payload");
        }
    }

    public static JsonObject parseClientData(JWSObject clientJwt, JsonObject extraData, BedrockSession session) throws Exception {
        JsonObject clientData = (JsonObject) JsonParser.parseString(clientJwt.getPayload().toString());
        ProxyConfig config = ProxyServer.getInstance().getConfiguration();
        if (config.useLoginExtras()) {
            // Add waterdog attributes
            clientData.addProperty("Waterdog_XUID", extraData.get("XUID").getAsString());
            clientData.addProperty("Waterdog_IP", ((InetSocketAddress) session.getSocketAddress()).getAddress().getHostAddress());
        }
        return clientData;
    }

    public static JsonObject parseExtraData(JsonObject payload) {
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

        ServerToClientHandshakePacket packet = new ServerToClientHandshakePacket();
        packet.setJwt(EncryptionUtils.createHandshakeJwt(privateKeyPair, token));

        session.getPeer().getChannel().eventLoop().execute(() -> {
            session.sendPacketImmediately(packet);
            session.enableEncryption(encryptionKey);
        });
    }
}
