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
import pe.waterdog.network.protocol.ProtocolVersion;

public abstract class BedrockCodec {

    private final ProtocolVersion protocol;
    private BedrockPacketCodec packetCodec;

    public BedrockCodec(ProtocolVersion protocol){
        this.protocol = protocol;
    }

    public BedrockPacketCodec.Builder createBuilder(String minecraftVer){
        BedrockPacketCodec.Builder builder = BedrockPacketCodec.builder();
        builder.protocolVersion(this.protocol.getProtocol());
        builder.raknetProtocolVersion(this.protocol.getRaknetVersion());
        builder.minecraftVersion(minecraftVer);
        return builder;
    }

    public BedrockPacketCodec.Builder createBuilder(BedrockPacketCodec defaultCodec){
        BedrockPacketCodec.Builder builder = BedrockPacketCodec.builder();
        builder.protocolVersion(defaultCodec.getProtocolVersion());
        builder.raknetProtocolVersion(defaultCodec.getRaknetProtocolVersion());
        builder.minecraftVersion(defaultCodec.getMinecraftVersion());
        return builder;
    }

    public void buildCodec(BedrockPacketCodec.Builder builder){
        this.packetCodec = builder.build();
    }

    public ProtocolVersion getProtocol() {
        return this.protocol;
    }

    public BedrockPacketCodec getPacketCodec() {
        return this.packetCodec;
    }
}
