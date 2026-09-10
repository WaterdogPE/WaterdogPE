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

import org.cloudburstmc.netty.channel.nethernet.signaling.NetherNetServerSignaling.PongData;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

/**
 * Drives NetherNet signaling from outside the proxy.
 * <p>
 * With {@code signaling.mode: external} the proxy binds no HTTP endpoint and a plugin feeds it
 * offers through here over whatever transport it likes. This is also the receiving half of a load
 * balanced setup: one node terminates signaling and hands offers to another through this API, and
 * the client's media connects straight to the node that answered.
 */
public interface SignalingService {

    /**
     * What an external endpoint should answer {@code GET /v1/join} with. The client only reads the
     * status, but a body keeps the endpoint useful to look at.
     */
    PongData advertisement(InetSocketAddress client);

    /**
     * Answers an SDP offer. The returned SDP already carries every ICE candidate and the operator
     * identity assertion, so it can be written to the client verbatim.
     *
     * @param networkId     the client's NetworkID, from the request path
     * @param offer         the raw SDP offer
     * @param clientAddress the client address, used for security decisions and reported to plugins
     */
    CompletableFuture<String> acceptOffer(String networkId, String offer, InetSocketAddress clientAddress);

    /**
     * Whether NetherNet is bound and below its connection limit.
     */
    boolean acceptsConnections();
}
