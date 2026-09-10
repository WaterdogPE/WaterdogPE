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

package dev.waterdog.waterdogpe.network.connection;

/**
 * What a transport provides underneath the Bedrock protocol.
 *
 * @param codecVersion       which framing, batching and compression rules apply, expressed as the
 *                           RakNet protocol version that introduced them
 * @param supportsEncryption whether Bedrock packet encryption is used on top of the transport
 */
public record TransportProfile(int codecVersion, boolean supportsEncryption) {

    /**
     * NetherNet follows RakNet protocol 11 framing and carries no Bedrock encryption: the data
     * channel is already inside DTLS, and a real client
     * answers {@code ServerToClientHandshake} with a plaintext {@code ClientToServerHandshake}.
     */
    public static final TransportProfile NETHERNET = new TransportProfile(11, false);

    public static TransportProfile raknet(int protocolVersion) {
        return new TransportProfile(protocolVersion, true);
    }
}
