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
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.cloudburstmc.netty.util.nethernet.IdentityUtils;
import org.cloudburstmc.netty.util.nethernet.TokenTrust;
import org.jose4j.jwt.JwtClaims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientAssertionFactoryTest {

    private static final String FINGERPRINT =
            "BA:02:D4:8A:F3:5B:99:20:E5:A6:89:74:36:AC:4F:55:EF:AB:EC:9B:9C:3F:7A:A1:B3:40:B9:A7:E1:1E:B1:80";

    private KeyPair keyPair;

    @BeforeEach
    void generateKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp384r1"));
        this.keyPair = generator.generateKeyPair();
    }

    /** Shaped like a real client offer, down to the reversed envelope key order. */
    private String offer(String fingerprint, boolean identity) throws Exception {
        String sdp = String.join("\r\n",
                "v=0",
                "o=- 1 2 IN IP4 127.0.0.1",
                "s=-",
                "t=0 0",
                "a=group:BUNDLE 0",
                "%IDENTITY%m=application 38992 UDP/DTLS/SCTP webrtc-datachannel",
                "c=IN IP4 172.20.0.1",
                "a=ice-ufrag:S2gu",
                "a=fingerprint:sha-256 " + fingerprint,
                "a=setup:actpass",
                "a=mid:0") + "\r\n";
        if (!identity) {
            return sdp.replace("%IDENTITY%", "");
        }
        // The assertion signs the fingerprints of the finished offer, so build it first.
        String unsigned = sdp.replace("%IDENTITY%", "");
        return sdp.replace("%IDENTITY%", "a=identity:" + envelope(unsigned) + "\r\n");
    }

    private String envelope(String sdp) throws Exception {
        String cpk = Base64.getEncoder().encodeToString(this.keyPair.getPublic().getEncoded());

        SignedJWT token = new SignedJWT(new JWSHeader(JWSAlgorithm.ES384), new JWTClaimsSet.Builder()
                .claim("cpk", cpk)
                .claim("xid", "1234567891234678")
                .claim("xname", "Tester")
                .issuer("https://authorization.franchise.minecraft-services.net/")
                .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
                .build());
        token.sign(new ECDSASigner((ECPrivateKey) this.keyPair.getPrivate()));

        JWSObject signed = new JWSObject(new JWSHeader(JWSAlgorithm.ES384),
                new Payload(IdentityUtils.getCanonicalFingerprintJson(sdp)));
        signed.sign(new ECDSASigner((ECPrivateKey) this.keyPair.getPrivate()));
        String[] parts = signed.serialize().split("\\.");
        String detached = parts[0] + ".." + parts[2];

        JsonObject assertion = new JsonObject();
        assertion.addProperty("fingerprints", detached);
        assertion.addProperty("token", token.serialize());

        JsonObject idp = new JsonObject();
        idp.addProperty("domain", "https://authorization.franchise.minecraft-services.net/");
        idp.addProperty("protocol", "default");

        JsonObject envelope = new JsonObject();
        envelope.addProperty("assertion", assertion.toString());
        envelope.add("idp", idp);
        return Base64.getEncoder().encodeToString(envelope.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void assertionsWeProduceValidateAgainstTheTransport() throws Exception {
        // The proxy signs one of these for every downstream connection. The transport verifies it
        // the same way a server verifies a client's, so this is the contract that has to hold.
        String sdp = offer(FINGERPRINT, false);
        String value = ClientAssertionFactory.create(sdp, this.keyPair, "1234567891234678", "Tester", "waterdog.test");

        JwtClaims claims = IdentityUtils.validateSdp(SdpUtil.withIdentity(sdp, value), TokenTrust.ANY);

        assertEquals("1234567891234678", claims.getClaimValueAsString("xid"));
        assertEquals("Tester", claims.getClaimValueAsString("xname"));
        assertEquals("waterdog.test", claims.getIssuer());
    }

    @Test
    void assertionsAreBoundToTheirOwnFingerprints() throws Exception {
        // Signed over one offer, presented with another, which is what a replay looks like
        String signed = offer(FINGERPRINT, false);
        String value = ClientAssertionFactory.create(signed, this.keyPair, "1234567891234678", "Tester", "waterdog.test");
        String other = offer("AA:" + FINGERPRINT.substring(3), false);

        assertThrows(Exception.class,
                () -> IdentityUtils.validateSdp(SdpUtil.withIdentity(other, value), TokenTrust.ANY));
    }

    @Test
    void identityIsInsertedAheadOfTheFirstMediaLine() throws Exception {
        String sdp = offer(FINGERPRINT, false);
        String withIdentity = SdpUtil.withIdentity(sdp, "abc");
        assertTrue(withIdentity.indexOf("a=identity:abc") < withIdentity.indexOf("m=application"));
    }

    @Test
    void appendsCandidatesAndEndOfCandidates() throws Exception {
        String sdp = offer(FINGERPRINT, false);
        String out = SdpUtil.withCandidates(sdp, java.util.List.of("a=candidate:1 1 udp 1 10.0.0.1 1 typ host"));

        assertTrue(out.contains("a=candidate:1 1 udp 1 10.0.0.1 1 typ host"));
        assertTrue(out.contains("a=end-of-candidates"));
    }
}
