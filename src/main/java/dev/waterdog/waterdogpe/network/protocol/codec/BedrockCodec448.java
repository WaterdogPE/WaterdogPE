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

package dev.waterdog.waterdogpe.network.protocol.codec;

import com.nukkitx.protocol.bedrock.BedrockPacketCodec;
import com.nukkitx.protocol.bedrock.BedrockPacketType;
import com.nukkitx.protocol.bedrock.packet.*;
import com.nukkitx.protocol.bedrock.v448.BedrockPacketHelper_v448;
import com.nukkitx.protocol.bedrock.v448.serializer.*;
import dev.waterdog.waterdogpe.network.protocol.ProtocolVersion;

public class BedrockCodec448 extends BedrockCodec440 {

    @Override
    public ProtocolVersion getProtocol() {
        return ProtocolVersion.MINECRAFT_PE_1_17_10;
    }

    @Override
    public void buildCodec(BedrockPacketCodec.Builder builder) {
        super.buildCodec(builder);
        builder.helper(BedrockPacketHelper_v448.INSTANCE);

        builder.deregisterPacket(ResourcePacksInfoPacket.class);
        builder.registerPacket(ResourcePacksInfoPacket.class, ResourcePacksInfoSerializer_v448.INSTANCE, 6);

        builder.deregisterPacket(SetTitlePacket.class);
        builder.registerPacket(SetTitlePacket.class, SetTitleSerializer_v448.INSTANCE, 88);

        builder.deregisterPacket(NpcRequestPacket.class);
        builder.registerPacket(NpcRequestPacket.class, NpcRequestSerializer_v448.INSTANCE, 98);

        builder.registerPacket(NpcDialoguePacket.class, NpcDialogueSerializer_v448.INSTANCE, 169);
    }

    @Override
    public void registerCommands(BedrockPacketCodec.Builder builder) {
        builder.registerPacket(AvailableCommandsPacket.class, AvailableCommandsSerializer_v448.INSTANCE, 76);
    }
}
