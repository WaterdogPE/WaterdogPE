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

import io.netty.channel.Channel;
import org.cloudburstmc.netty.channel.nethernet.NetherNetChildChannel;
import org.cloudburstmc.netty.util.nethernet.PlayerInfo;

import java.security.GeneralSecurityException;
import java.security.interfaces.ECPublicKey;

/**
 * Ties the identity that opened the transport to the identity the login chain names.
 * <p>
 * The NetherNet onboarding guide binds the two deliberately: the assertion's {@code cpk} signs the
 * offer's DTLS fingerprints, and it is the same key the Bedrock login chain is signed with. On a
 * transport that carries Bedrock encryption the handshake already enforces that, because the
 * session key comes out of an ECDH against the chain's {@code identityPublicKey}. NetherNet carries
 * no Bedrock encryption, so the assertion is what enforces it there instead.
 * <p>
 * Checking both halves agree is what makes a login chain meaningful rather than merely well formed.
 */
public final class TransportIdentityBinding {

    private TransportIdentityBinding() {
    }

    /**
     * @param channel            the upstream channel the login arrived on
     * @param identityPublicKey  the key the login chain is signed with
     * @return null when the two identities agree, otherwise why the login should be refused
     */
    public static String mismatch(Channel channel, ECPublicKey identityPublicKey) {
        PlayerInfo player = channel.attr(NetherNetChildChannel.PLAYER_INFO).get();
        if (player == null) {
            return "the transport carries no validated identity to bind the login chain to";
        }

        try {
            if (!player.clientPublicKey().equals(identityPublicKey)) {
                return "the login chain is signed with a different key than the one that opened the transport";
            }
        } catch (GeneralSecurityException e) {
            return "the transport identity has no usable key: " + e.getMessage();
        }
        return null;
    }
}
