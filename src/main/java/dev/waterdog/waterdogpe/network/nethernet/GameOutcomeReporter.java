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
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.AttributeKey;
import org.cloudburstmc.netty.signaling.ProviderClient;
import org.cloudburstmc.netty.signaling.admission.AdmissionPrincipal;
import org.cloudburstmc.protocol.bedrock.netty.BedrockBatchWrapper;
import org.cloudburstmc.protocol.bedrock.netty.BedrockPacketWrapper;
import org.cloudburstmc.protocol.bedrock.packet.DisconnectPacket;
import org.cloudburstmc.protocol.bedrock.packet.RequestNetworkSettingsPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetLocalPlayerAsInitializedPacket;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntPredicate;

/**
 * Game outcomes for players the provider admitted, reported as Geyser reports them: joined once the
 * client has loaded into its first server, rejected when the proxy disconnects it before that.
 */
public final class GameOutcomeReporter {
    public static final String HANDLER_NAME = "nethernet-game-outcome";
    private static final AttributeKey<State> STATE = AttributeKey.valueOf(GameOutcomeReporter.class, "state");
    private final Sink sink;
    private final AtomicLong dropped = new AtomicLong();

    /**
     * Where outcomes go, normally {@link ProviderClient#reportGameOutcome}.
     */
    @FunctionalInterface
    public interface Sink {
        /**
         * @return false when the outcome was dropped
         */
        boolean report(String ticketId, ProviderClient.GameOutcome outcome, String reason);
    }

    public GameOutcomeReporter(Sink sink) {
        this.sink = sink;
    }

    // Owned by the channel: no global session map or references to native resources.
    private static final class State {
        boolean unsupportedVersion;
        boolean reported;
    }

    /**
     * @param supportedProtocol whether the proxy accepts a client protocol version
     */
    public ChannelDuplexHandler observer(IntPredicate supportedProtocol) {
        return new ChannelDuplexHandler() {
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object message) throws Exception {
                for (BedrockPacketWrapper wrapper : primary(message)) {
                    if (wrapper.getPacket() instanceof RequestNetworkSettingsPacket settings) {
                        State state = state(ctx.channel());
                        if (state != null) synchronized (state) {
                            state.unsupportedVersion = !supportedProtocol.test(settings.getProtocolVersion());
                        }
                    } else if (wrapper.getPacket() instanceof SetLocalPlayerAsInitializedPacket) {
                        // The client sends this once it has spawned into the server the proxy picked
                        report(ctx.channel(), true);
                    }
                }
                ctx.fireChannelRead(message);
            }

            @Override
            public void write(ChannelHandlerContext ctx, Object message, ChannelPromise promise) throws Exception {
                for (BedrockPacketWrapper wrapper : primary(message)) {
                    if (wrapper.getPacket() instanceof DisconnectPacket) {
                        report(ctx.channel(), false);
                    }
                }
                ctx.write(message, promise);
            }
        };
    }

    /** Packets of the main client, whether they pass one at a time or in a batch. */
    private static List<BedrockPacketWrapper> primary(Object message) {
        List<BedrockPacketWrapper> packets = message instanceof BedrockBatchWrapper batch ? batch.getPackets()
                : message instanceof BedrockPacketWrapper wrapper ? List.of(wrapper) : List.of();
        return packets.stream()
                .filter(wrapper -> wrapper.getSenderSubClientId() == 0 && wrapper.getTargetSubClientId() == 0)
                .toList();
    }

    private static State state(Channel channel) {
        if (channel.attr(AdmissionPrincipal.KEY).get() == null) return null;
        var attribute = channel.attr(STATE);
        State state = attribute.get();
        if (state != null) return state;
        State created = new State();
        State previous = attribute.setIfAbsent(created);
        return previous == null ? created : previous;
    }

    private void report(Channel channel, boolean joined) {
        State state = state(channel);
        if (state == null) return;
        synchronized (state) {
            if (state.reported) return;
            state.reported = true; // A dropped outcome must not turn a later disconnect into rejection.
            // Never publish disconnect text, player identity, or game payloads.
            if (!this.sink.report(channel.attr(AdmissionPrincipal.KEY).get().ticketId(),
                    joined ? ProviderClient.GameOutcome.JOINED : ProviderClient.GameOutcome.REJECTED,
                    joined ? "play_ready" : state.unsupportedVersion ? "unsupported_version" : "server_rejected")) {
                this.dropped.incrementAndGet();
            }
        }
    }

    public long droppedEvents() {
        return this.dropped.get();
    }
}
