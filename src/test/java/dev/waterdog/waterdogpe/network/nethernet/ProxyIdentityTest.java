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

import org.cloudburstmc.netty.util.nethernet.IdentityUtils;
import org.cloudburstmc.netty.util.nethernet.TokenTrust;
import org.jose4j.jwt.JwtClaims;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ProxyIdentityTest {
    private static final String SDP = "v=0\r\no=- 1 2 IN IP4 127.0.0.1\r\n"
            + "a=fingerprint:sha-256 AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99:"
            + "AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99\r\n"
            + "m=application 9 UDP/DTLS/SCTP webrtc-datachannel\r\n";

    /** The offer is signed with, and names, the key the downstream login is signed with. */
    @Test
    public void theConnectionPresentsTheLoginKey() throws Exception {
        // As the login's, without EncryptionUtils, which fetches Mojang's keys when loaded
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp384r1"));
        KeyPair login = generator.generateKeyPair();
        String offer = ProxyIdentity.forPlayer(login, "WaterdogPE", "2535000000000000", "Steve").withAssertion(SDP);

        JwtClaims claims = IdentityUtils.validateSdp(offer, TokenTrust.ANY);
        assertArrayEquals(login.getPublic().getEncoded(),
                IdentityUtils.decodePublicKey(claims.getClaimValueAsString("cpk")).getEncoded());
        assertEquals("2535000000000000", claims.getClaimValueAsString("xid"));
        assertEquals("Steve", claims.getClaimValueAsString("xname"));
    }
}
