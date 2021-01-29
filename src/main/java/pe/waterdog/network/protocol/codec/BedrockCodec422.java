/**
 * Copyright 2020 WaterdogTEAM
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pe.waterdog.network.protocol.codec;

import com.nukkitx.protocol.bedrock.BedrockPacketCodec;
import com.nukkitx.protocol.bedrock.packet.ItemStackRequestPacket;
import com.nukkitx.protocol.bedrock.packet.ResourcePacksInfoPacket;
import com.nukkitx.protocol.bedrock.v422.serializer.ItemStackRequestSerializer_v422;
import com.nukkitx.protocol.bedrock.v422.serializer.ResourcePacksInfoSerializer_v422;
import pe.waterdog.network.protocol.ProtocolVersion;

public class BedrockCodec422 extends BedrockCodec419 {

    @Override
    public ProtocolVersion getProtocol() {
        return ProtocolVersion.MINECRAFT_PE_1_16_200;
    }

    @Override
    public void buildCodec(BedrockPacketCodec.Builder builder) {
        super.buildCodec(builder);
        builder.deregisterPacket(ResourcePacksInfoPacket.class);
        builder.registerPacket(ResourcePacksInfoPacket.class, ResourcePacksInfoSerializer_v422.INSTANCE, 6);
    }

    @Override
    public void registerItemPackets(BedrockPacketCodec.Builder builder) {
        super.registerItemPackets(builder);
        builder.deregisterPacket(ItemStackRequestPacket.class);
        builder.registerPacket(ItemStackRequestPacket.class, ItemStackRequestSerializer_v422.INSTANCE, 147);
    }
}
