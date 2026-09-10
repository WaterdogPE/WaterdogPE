/*
 * Copyright 2026 WaterdogTEAM
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

package dev.waterdog.waterdogpe.network.nethernet;

import com.google.gson.JsonObject;
import org.cloudburstmc.netty.util.nethernet.IdentityUtils;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

/**
 * Builds the {@code a=identity} assertion the proxy presents when it connects out over NetherNet.
 * <p>
 * A server that requires an assertion checks that it is well formed and that the detached JWS over
 * the offer's fingerprints verifies against the token's {@code cpk}. The token itself is not
 * checked against the Minecraft auth service, so the proxy signs its own with the same keypair it
 * identifies itself to clients with, and carries the player's identity in the claims.
 */
public final class ClientAssertionFactory {

    private static final long LIFETIME_SECONDS = 3600;

    private ClientAssertionFactory() {
    }

    /**
     * @param offerSdp the offer whose fingerprints the assertion binds to, without an identity line
     * @param xuid     the connecting player's XUID, surfaced to the downstream server
     * @param name     the connecting player's name
     */
    public static String create(String offerSdp, KeyPair keyPair, String xuid, String name, String domain) throws Exception {
        ECDSASigner signer = new ECDSASigner((ECPrivateKey) keyPair.getPrivate());
        String cpk = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

        Instant now = Instant.now();
        SignedJWT token = new SignedJWT(new JWSHeader(JWSAlgorithm.ES384), new JWTClaimsSet.Builder()
                .claim("cpk", cpk)
                .claim("xid", xuid == null ? "" : xuid)
                .claim("xname", name == null ? "" : name)
                .issuer(domain)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(LIFETIME_SECONDS)))
                .build());
        token.sign(signer);

        JWSObject fingerprints = new JWSObject(new JWSHeader(JWSAlgorithm.ES384),
                new Payload(IdentityUtils.getCanonicalFingerprintJson(offerSdp)));
        fingerprints.sign(signer);
        String[] parts = fingerprints.serialize().split("\\.");

        JsonObject assertion = new JsonObject();
        assertion.addProperty("fingerprints", parts[0] + ".." + parts[2]);
        assertion.addProperty("token", token.serialize());

        JsonObject idp = new JsonObject();
        idp.addProperty("domain", domain);
        idp.addProperty("protocol", "default");

        JsonObject envelope = new JsonObject();
        envelope.addProperty("assertion", assertion.toString());
        envelope.add("idp", idp);
        return Base64.getEncoder().encodeToString(envelope.toString().getBytes(StandardCharsets.UTF_8));
    }
}
