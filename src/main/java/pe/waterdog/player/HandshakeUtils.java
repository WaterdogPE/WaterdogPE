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

import com.nimbusds.jose.*;
import com.nukkitx.protocol.bedrock.BedrockSession;
import com.nukkitx.protocol.bedrock.packet.LoginPacket;
import com.nukkitx.protocol.bedrock.util.EncryptionUtils;
import net.minidev.json.JSONObject;
import pe.waterdog.ProxyServer;
import pe.waterdog.utils.ProxyConfig;

import java.net.URI;
import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;
import java.util.UUID;

public class HandshakeUtils {

    public static JWSObject createExtraData(KeyPair pair, JSONObject extraData) {
        String publicKeyBase64 = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());

        long timestamp = System.currentTimeMillis() / 1000;

        JSONObject dataChain = new JSONObject();
        dataChain.put("nbf", timestamp - 3600);
        dataChain.put("exp", timestamp + 24 * 3600);
        dataChain.put("iat", timestamp);
        dataChain.put("iss", "self");
        dataChain.put("certificateAuthority", true);
        dataChain.put("extraData", extraData);
        dataChain.put("randomNonce", UUID.randomUUID().getLeastSignificantBits());
        dataChain.put("identityPublicKey", publicKeyBase64);

        return encodeJWT(pair, dataChain);
    }

    public static JWSObject encodeJWT(KeyPair pair, JSONObject payload) {
        String publicKeyBase64 = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
        URI x5u = URI.create(publicKeyBase64);
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES384).x509CertURL(x5u).build();

        JWSObject jwsObject = new JWSObject(header, new Payload(payload));

        try {
            EncryptionUtils.signJwt(jwsObject, (ECPrivateKey) pair.getPrivate());
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }

        return jwsObject;
    }

    public static JSONObject parseClientData(LoginPacket packet, JSONObject payload, BedrockSession session) throws Exception {
        String identityPublicKeyString = payload.getAsString("identityPublicKey");
        if (identityPublicKeyString == null) {
            throw new RuntimeException("Identity Public Key was not found!");
        }

        ECPublicKey identityPublicKey = EncryptionUtils.generateKey(identityPublicKeyString);

        JWSObject clientJwt = JWSObject.parse(packet.getSkinData().toString());
        EncryptionUtils.verifyJwt(clientJwt, identityPublicKey);

        JSONObject clientData = clientJwt.getPayload().toJSONObject();

        /* Add WaterdogAttributes*/
        ProxyConfig config = ProxyServer.getInstance().getConfiguration();
        if (config.useLoginExtras() && config.isIpForward()) {
            clientData.put("Waterdog_IP", session.getAddress().getAddress().getHostAddress());
        }

        return clientData;
    }

    public static JSONObject parseExtraData(LoginPacket packet, JSONObject payload) {
        Object extraDataObject = payload.get("extraData");
        if (!(extraDataObject instanceof JSONObject)) {
            throw new IllegalStateException("Invalid 'extraData'");
        }

        JSONObject extraData = (JSONObject) extraDataObject;
        /* Replace spaces in name*/
        if (ProxyServer.getInstance().getConfiguration().isReplaceUsernameSpaces()) {
            String playerName = extraData.getAsString("displayName");
            extraData.put("displayName", playerName.replaceAll(" ", "_"));
        }

        return extraData;
    }
}
