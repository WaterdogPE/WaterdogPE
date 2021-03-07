/*
 * Copyright 2021 WaterdogTEAM
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

package dev.waterdog.network.protocol.codec;

import com.nukkitx.protocol.bedrock.BedrockPacketCodec;
import com.nukkitx.protocol.bedrock.packet.AvailableCommandsPacket;
import com.nukkitx.protocol.bedrock.v340.BedrockPacketHelper_v340;
import com.nukkitx.protocol.bedrock.v340.serializer.AvailableCommandsSerializer_v340;
import dev.waterdog.network.protocol.ProtocolVersion;

public class BedrockCodec340 extends BedrockCodec332 {

    @Override
    public ProtocolVersion getProtocol() {
        return ProtocolVersion.MINECRAFT_PE_1_10;
    }

    @Override
    public void buildCodec(BedrockPacketCodec.Builder builder) {
        super.buildCodec(builder);
        builder.helper(BedrockPacketHelper_v340.INSTANCE);
    }

    @Override
    public void registerCommands(BedrockPacketCodec.Builder builder) {
        builder.registerPacket(AvailableCommandsPacket.class, AvailableCommandsSerializer_v340.INSTANCE, 76);
    }
}
