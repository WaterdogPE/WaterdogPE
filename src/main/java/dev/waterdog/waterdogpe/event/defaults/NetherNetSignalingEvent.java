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

package dev.waterdog.waterdogpe.event.defaults;

import dev.waterdog.waterdogpe.event.CancellableEvent;
import dev.waterdog.waterdogpe.event.Event;
import org.cloudburstmc.netty.util.nethernet.PlayerInfo;
import lombok.Getter;
import lombok.Setter;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

/**
 * Called when a client posts an SDP offer to {@code POST /v1/join/{networkId}}.
 * <p>
 * Signaling is one HTTP round trip and the media path runs directly between client and host, so
 * answering an offer on behalf of another node is enough to move the whole session there. Set
 * {@link #setAnswer(CompletableFuture)} and this proxy creates no peer connection at all, it just
 * relays the answer, and the client's WebRTC connection goes straight to whoever produced it.
 * That is the hook for custom load balancing.
 * <p>
 * Cancelling responds with a non 2xx instead.
 */
@Getter
public class NetherNetSignalingEvent extends Event implements CancellableEvent {

    private final InetSocketAddress address;
    private final String networkId;
    private final String offer;
    /**
     * The verified client identity, or null when the offer carried none.
     */
    private final PlayerInfo player;

    @Setter
    private CompletableFuture<String> answer;

    public NetherNetSignalingEvent(InetSocketAddress address, String networkId, String offer, PlayerInfo player) {
        this.address = address;
        this.networkId = networkId;
        this.offer = offer;
        this.player = player;
    }
}
