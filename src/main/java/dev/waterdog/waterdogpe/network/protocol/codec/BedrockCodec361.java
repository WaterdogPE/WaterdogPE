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
import com.nukkitx.protocol.bedrock.packet.*;
import com.nukkitx.protocol.bedrock.v361.BedrockPacketHelper_v361;
import com.nukkitx.protocol.bedrock.v361.serializer.*;
import dev.waterdog.waterdogpe.network.protocol.ProtocolVersion;

public class BedrockCodec361 extends BedrockCodec354 {

    @Override
    public ProtocolVersion getProtocol() {
        return ProtocolVersion.MINECRAFT_PE_1_12;
    }

    @Override
    public void buildCodec(BedrockPacketCodec.Builder builder) {
        super.buildCodec(builder);
        builder.helper(BedrockPacketHelper_v361.INSTANCE);

        builder.deregisterPacket(ResourcePackDataInfoPacket.class);
        builder.registerPacket(ResourcePackDataInfoPacket.class, ResourcePackDataInfoSerializer_v361.INSTANCE, 82);

        builder.deregisterPacket(StartGamePacket.class);
        builder.registerPacket(StartGamePacket.class, StartGameSerializer_v361.INSTANCE, 11);

        builder.deregisterPacket(AddPaintingPacket.class);
        builder.registerPacket(AddPaintingPacket.class, AddPaintingSerializer_v361.INSTANCE, 22);

        builder.deregisterPacket(LevelChunkPacket.class);
        builder.registerPacket(LevelChunkPacket.class, LevelChunkSerializer_v361.INSTANCE, 58);

        builder.registerPacket(ClientCacheStatusPacket.class, ClientCacheStatusSerializer_v361.INSTANCE, 129);
        builder.registerPacket(ClientCacheBlobStatusPacket.class, ClientCacheBlobStatusSerializer_v361.INSTANCE, 135);
        builder.registerPacket(ClientCacheMissResponsePacket.class, ClientCacheMissResponseSerializer_v361.INSTANCE, 136);
    }
}
