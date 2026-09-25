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

import dev.waterdog.waterdogpe.network.protocol.ProtocolCodecs;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodecHelper;
import org.cloudburstmc.protocol.bedrock.codec.v2168.Bedrock_v2168;
import org.cloudburstmc.protocol.bedrock.data.event.ComposterInteractEventData;
import org.cloudburstmc.protocol.bedrock.data.event.MobKilledEventData;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.EventPacket;
import org.cloudburstmc.protocol.common.util.VarInts;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Composter events captured off a Bedrock Dedicated Server have to reach the client as the bytes they
 * arrived as.
 *
 * <p>Both payloads below kicked the player who clicked the composter and nobody else. The codec
 * reads the interaction type with the zigzag reader although it is an unsigned index: filling the
 * composter sends 19, which became -10 and failed to decode, and taking the bone meal back out
 * sends 20, which decoded into the wrong constant and re-encoded a byte short.</p>
 */
class EventRelaySerializerTest {

    private static final int EVENT_PACKET_ID = 65;

    /** Taking the bone meal out of a full composter: 13 bytes in, 12 bytes back out before this. */
    private static final String RECOVERED_BONEMEAL_WIRE = "fdffffffdf800120010b141801";

    /** The same event with the fill's interaction type, 19, which the zigzag reader made -10. */
    private static final String COMPOST_ITEM_PLACE_WIRE = "fdffffffdf800120010b131801";

    /** Everything behind the entity id, which is all of the packet the proxy relays. */
    private static final String PAYLOAD_WIRE = "20010b141801";

    private final BedrockCodec codec = ProtocolCodecs.buildCodec(Bedrock_v2168.CODEC);
    private final BedrockCodecHelper helper = this.codec.createHelper();

    @Test
    void relaysBoneMealRecoveryUnchanged() {
        EventPacket packet = this.decode(RECOVERED_BONEMEAL_WIRE);
        assertEquals(RECOVERED_BONEMEAL_WIRE, this.encode(packet));
    }

    /**
     * The interaction type the codec cannot read at all. Decoding it threw, and the proxy turned
     * that into a disconnect.
     */
    @Test
    void relaysAnInteractionTypeTheCodecCannotRead() {
        EventPacket packet = assertDoesNotThrow(() -> this.decode(COMPOST_ITEM_PLACE_WIRE));
        assertNull(packet.getEventData(), "a payload that does not decode leaves no model behind");
        assertEquals(COMPOST_ITEM_PLACE_WIRE, this.encode(packet));
    }

    /**
     * The payload is still decoded for handlers and plugins to read - it is just not what goes back
     * on the wire. The constant it resolves to is the codec's business, and getting it wrong is
     * exactly why the model is not re-encoded.
     */
    @Test
    void stillDecodesThePayloadForHandlers() {
        EventPacket packet = this.decode(RECOVERED_BONEMEAL_WIRE);

        ComposterInteractEventData eventData =
                assertInstanceOf(ComposterInteractEventData.class, packet.getEventData());
        assertEquals(12, eventData.getItemId());
    }

    /**
     * The one field the proxy owns: rewriting the entity id must not disturb the payload behind it,
     * even when the new id encodes to a different length.
     */
    @Test
    void rewritingTheEntityIdKeepsThePayload() {
        EventPacket packet = this.decode(RECOVERED_BONEMEAL_WIRE);
        packet.setUniqueEntityId(7);

        assertEquals(this.hexOfVarLong(7) + PAYLOAD_WIRE, this.encode(packet));
    }

    /**
     * Replacing the event data means the caller wants their version on the wire, so the bytes this
     * packet arrived as stop being relayed.
     */
    @Test
    void replacingTheEventDataTakesOverTheWire() {
        EventPacket packet = this.decode(RECOVERED_BONEMEAL_WIRE);
        packet.setEventData(new MobKilledEventData(7L, 9L, 3, 4));

        assertNotEquals(RECOVERED_BONEMEAL_WIRE, this.encode(packet));
    }

    /** A packet a plugin built never saw the wire, so the codec's own serializer still writes it. */
    @Test
    void encodesAPacketBuiltByAPlugin() {
        EventPacket packet = new EventPacket();
        packet.setUniqueEntityId(7);
        packet.setEventData(new MobKilledEventData(7L, 9L, 3, 4));

        String wire = assertDoesNotThrow(() -> this.encode(packet));
        assertInstanceOf(MobKilledEventData.class, this.decode(wire).getEventData());
    }

    private EventPacket decode(String hex) {
        ByteBuf buffer = Unpooled.wrappedBuffer(ByteBufUtil.decodeHexDump(hex));
        try {
            BedrockPacket packet = this.codec.tryDecode(this.helper, buffer, EVENT_PACKET_ID, null);
            return assertInstanceOf(EventPacket.class, packet);
        } finally {
            buffer.release();
        }
    }

    private String encode(EventPacket packet) {
        ByteBuf buffer = Unpooled.buffer();
        try {
            this.codec.tryEncode(this.helper, buffer, packet);
            return ByteBufUtil.hexDump(buffer);
        } finally {
            buffer.release();
        }
    }

    private String hexOfVarLong(long value) {
        ByteBuf buffer = Unpooled.buffer();
        try {
            VarInts.writeLong(buffer, value);
            return ByteBufUtil.hexDump(buffer);
        } finally {
            buffer.release();
        }
    }
}
