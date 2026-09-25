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

import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.codec.BedrockPacketDefinition;
import org.cloudburstmc.protocol.bedrock.packet.EventPacket;

/**
 * Hands {@link EventPacket} to {@link EventRelaySerializer}, so its payload crosses the proxy as the
 * bytes it arrived as instead of being written back out from the codec's model of it.
 */
public class CodecUpdaterEvents implements ProtocolCodecUpdater {

    @Override
    public BedrockCodec.Builder updateCodec(BedrockCodec.Builder builder, BedrockCodec baseCodec) {
        BedrockPacketDefinition<EventPacket> definition = baseCodec.getPacketDefinition(EventPacket.class);
        if (definition == null) {
            return builder; // a version that does not know this packet at all
        }
        builder.updateSerializer(EventPacket.class, new EventRelaySerializer(definition.getSerializer()));
        builder.updateFactory(EventPacket.class, EventRelaySerializer.RelayedEventPacket::new);
        // The encoder looks a packet up by its exact class, so the subclass needs its own entry.
        // It has to be added last: aliasing copies the definition as it stands, and the two calls
        // above replace it.
        builder.aliasPacket(EventRelaySerializer.RelayedEventPacket.class, EventPacket.class);
        return builder;
    }
}
