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
import com.nukkitx.protocol.bedrock.packet.AddVolumeEntityPacket;
import com.nukkitx.protocol.bedrock.packet.BossEventPacket;
import com.nukkitx.protocol.bedrock.packet.LevelChunkPacket;
import com.nukkitx.protocol.bedrock.v486.BedrockPacketHelper_v486;
import com.nukkitx.protocol.bedrock.v486.serializer.AddVolumeEntitySerializer_v486;
import com.nukkitx.protocol.bedrock.v486.serializer.BossEventSerializer_v486;
import com.nukkitx.protocol.bedrock.v486.serializer.LevelChunkSerializer_v486;
import dev.waterdog.waterdogpe.network.protocol.ProtocolVersion;

public class BedrockCodec486 extends BedrockCodec475 {

    @Override
    public ProtocolVersion getProtocol() {
        return ProtocolVersion.MINECRAFT_PE_1_18_10;
    }

    @Override
    public void buildCodec(BedrockPacketCodec.Builder builder) {
        super.buildCodec(builder);
        builder.helper(BedrockPacketHelper_v486.INSTANCE);

        builder.deregisterPacket(AddVolumeEntityPacket.class);
        builder.registerPacket(AddVolumeEntityPacket.class, AddVolumeEntitySerializer_v486.INSTANCE, 166);

        builder.deregisterPacket(BossEventPacket.class);
        builder.registerPacket(BossEventPacket.class, BossEventSerializer_v486.INSTANCE, 74);

        builder.deregisterPacket(LevelChunkPacket.class);
        builder.registerPacket(LevelChunkPacket.class, LevelChunkSerializer_v486.INSTANCE, 58);
    }
}
