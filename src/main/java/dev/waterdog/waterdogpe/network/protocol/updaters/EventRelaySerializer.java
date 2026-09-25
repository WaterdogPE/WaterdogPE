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

package dev.waterdog.waterdogpe.network.protocol.updaters;

import io.netty.buffer.ByteBuf;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodecHelper;
import org.cloudburstmc.protocol.bedrock.codec.BedrockPacketSerializer;
import org.cloudburstmc.protocol.bedrock.data.event.EventData;
import org.cloudburstmc.protocol.bedrock.packet.EventPacket;
import org.cloudburstmc.protocol.common.util.VarInts;

/**
 * Relays {@link EventPacket} payloads byte for byte, rewriting only the entity id the proxy owns.
 * <p>
 * The proxy reads exactly one field of this packet: the unique entity id, which {@code EntityMap}
 * rewrites when it belongs to the player. Everything behind it was being parsed into the codec's
 * model and written back out from that model, which loses whatever the model does not describe.
 * Two ways that bites a backend the codec build is older than:
 * <ul>
 *     <li>The composter and cauldron interaction type is an <em>unsigned</em> varint index into a
 *     list the game extends between versions, and the codec reads it with the zigzag reader. Filling
 *     a composter sends 19, which zigzag turns into -10 and fails to decode; taking the bone meal
 *     back out sends 20, which decodes without complaint into the wrong constant.</li>
 *     <li>A payload with a field the codec build does not know re-encodes short: that same event
 *     arrives 13 bytes and goes back out as 12. The client closes the connection with no reason
 *     given, and only the player who clicked the composter is kicked.</li>
 * </ul>
 * The payload is still decoded into the packet, so handlers and plugins see exactly what they saw
 * before, but that model is only used for reading: an untouched packet goes back out as the bytes it
 * arrived as. A packet whose payload was replaced through {@link EventPacket#setEventData} - and one
 * a plugin built rather than decoded - is encoded by the codec's own serializer, unchanged.
 */
@Log4j2
public class EventRelaySerializer implements BedrockPacketSerializer<EventPacket> {

    private final BedrockPacketSerializer<EventPacket> parent;

    public EventRelaySerializer(BedrockPacketSerializer<EventPacket> parent) {
        this.parent = parent;
    }

    @Override
    public void serialize(ByteBuf buffer, BedrockCodecHelper helper, EventPacket packet) {
        byte[] payload = packet instanceof RelayedEventPacket relayed ? relayed.getRawPayload() : null;
        if (payload == null) {
            this.parent.serialize(buffer, helper, packet);
            return;
        }

        // The entity id is the first field in every version of this packet, so the payload behind it
        // goes back out untouched even when the rewritten id encodes to a different length.
        VarInts.writeLong(buffer, packet.getUniqueEntityId());
        buffer.writeBytes(payload);
    }

    @Override
    public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, EventPacket packet) {
        ByteBuf model = buffer.duplicate(); // shares the memory, owns its indexes

        packet.setUniqueEntityId(VarInts.readLong(buffer));
        byte[] payload = new byte[buffer.readableBytes()];
        buffer.readBytes(payload);

        try {
            this.parent.deserialize(model, helper, packet);
        } catch (Exception e) {
            // Only the model is lost. The payload still relays, and a handler that reads event data
            // it does not recognise has to deal with null anyway.
            log.debug("Could not decode the payload of an EventPacket, relaying it as received", e);
        }

        // After the decode: the setter below drops the payload when someone replaces the event data,
        // and the decode itself is the one caller that means no such thing.
        if (packet instanceof RelayedEventPacket relayed) {
            relayed.setRawPayload(payload);
        }
    }

    /**
     * An {@link EventPacket} that remembers the payload it was decoded from.
     *
     * <p>Registered as the factory for {@code EventPacket} and aliased back to it, so the packet
     * keeps its identity everywhere else: handlers, plugins and the encoder all still see an
     * {@code EventPacket}.</p>
     */
    public static class RelayedEventPacket extends EventPacket {

        private byte[] rawPayload;

        public byte[] getRawPayload() {
            return this.rawPayload;
        }

        public void setRawPayload(byte[] rawPayload) {
            this.rawPayload = rawPayload;
        }

        /**
         * Replacing the event data is the only way to change this packet's payload, and it means the
         * caller wants their version on the wire. Drop the bytes we were going to relay.
         */
        @Override
        public void setEventData(EventData eventData) {
            if (this.rawPayload != null && eventData != this.getEventData()) {
                this.rawPayload = null;
            }
            super.setEventData(eventData);
        }
    }
}
