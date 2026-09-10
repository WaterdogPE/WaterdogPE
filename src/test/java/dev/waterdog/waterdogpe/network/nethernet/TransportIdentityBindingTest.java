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

import io.netty.channel.embedded.EmbeddedChannel;
import org.cloudburstmc.netty.channel.nethernet.NetherNetChildChannel;
import org.cloudburstmc.netty.util.nethernet.IdentityUtils;
import org.cloudburstmc.netty.util.nethernet.PlayerInfo;
import org.cloudburstmc.netty.util.nethernet.TokenTrust;
import org.jose4j.jwt.JwtClaims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransportIdentityBindingTest {

    private static final String DOMAIN = "https://authorization.franchise.minecraft-services.net/";
    private static final String OFFER = String.join("\r\n",
            "v=0",
            "o=- 1 2 IN IP4 127.0.0.1",
            "s=-",
            "t=0 0",
            "a=fingerprint:sha-256 1B:2C:3D:4E:5F:60:71:82:93:A4:B5:C6:D7:E8:F9:0A"
                    + ":1B:2C:3D:4E:5F:60:71:82:93:A4:B5:C6:D7:E8:F9:0A",
            "m=application 9 UDP/DTLS/SCTP webrtc-datachannel",
            "c=IN IP4 0.0.0.0",
            "");

    private KeyPairGenerator generator;

    @BeforeEach
    void setUp() throws Exception {
        this.generator = KeyPairGenerator.getInstance("EC");
        this.generator.initialize(new ECGenParameterSpec("secp384r1"));
    }

    /** What the transport hands a consumer once it has validated the offer that opened a channel. */
    private PlayerInfo validatedIdentity(KeyPair pair) throws Exception {
        String envelope = ClientAssertionFactory.create(OFFER, pair, "1234567891234678", "someone", DOMAIN);
        JwtClaims claims = IdentityUtils.validateSdp(SdpUtil.withIdentity(OFFER, envelope), TokenTrust.ANY);
        return new PlayerInfo(claims.getClaimValueAsString("xid"), claims.getClaimValueAsString("xname"),
                "42", null, claims);
    }

    private EmbeddedChannel channelWith(PlayerInfo player) {
        EmbeddedChannel channel = new EmbeddedChannel();
        if (player != null) {
            channel.attr(NetherNetChildChannel.PLAYER_INFO).set(player);
        }
        return channel;
    }

    @Test
    void acceptsAChainSignedByTheKeyThatOpenedTheTransport() throws Exception {
        KeyPair pair = this.generator.generateKeyPair();
        EmbeddedChannel channel = this.channelWith(this.validatedIdentity(pair));
        assertNull(TransportIdentityBinding.mismatch(channel, (ECPublicKey) pair.getPublic()));
    }

    @Test
    void refusesAChainSignedByAnyOtherKey() throws Exception {
        // A chain whose key never took part in opening this transport.
        KeyPair transport = this.generator.generateKeyPair();
        KeyPair stolen = this.generator.generateKeyPair();
        EmbeddedChannel channel = this.channelWith(this.validatedIdentity(transport));

        String mismatch = TransportIdentityBinding.mismatch(channel, (ECPublicKey) stolen.getPublic());
        assertNotNull(mismatch);
        assertTrue(mismatch.contains("different key"));
    }

    @Test
    void refusesWhenTheTransportCarriesNoIdentity() throws Exception {
        KeyPair pair = this.generator.generateKeyPair();
        String mismatch = TransportIdentityBinding.mismatch(this.channelWith(null), (ECPublicKey) pair.getPublic());
        assertNotNull(mismatch);
        assertTrue(mismatch.contains("no validated identity"));
    }
}
