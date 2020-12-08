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

package pe.waterdog.network.protocol;

import com.nukkitx.protocol.bedrock.BedrockPacketCodec;
import com.nukkitx.protocol.bedrock.v388.Bedrock_v388;
import com.nukkitx.protocol.bedrock.v389.Bedrock_v389;
import com.nukkitx.protocol.bedrock.v390.Bedrock_v390;
import com.nukkitx.protocol.bedrock.v407.Bedrock_v407;
import com.nukkitx.protocol.bedrock.v408.Bedrock_v408;
import com.nukkitx.protocol.bedrock.v419.Bedrock_v419;
import com.nukkitx.protocol.bedrock.v422.Bedrock_v422;
import lombok.ToString;
import pe.waterdog.network.protocol.codec.BedrockCodec;

@ToString(exclude = {"defaultCodec", "bedrockCodec"})
public enum ProtocolVersion {

    MINECRAFT_PE_1_13(388, Bedrock_v388.V388_CODEC, 9),
    MINECRAFT_PE_1_14_30(389, Bedrock_v389.V389_CODEC, 9),
    MINECRAFT_PE_1_14_60(390, Bedrock_v390.V390_CODEC, 9),
    MINECRAFT_PE_1_16(407, Bedrock_v407.V407_CODEC, 10),
    MINECRAFT_PE_1_16_20(408, Bedrock_v408.V408_CODEC, 10),
    MINECRAFT_PE_1_16_100(419, Bedrock_v419.V419_CODEC, 10),
    MINECRAFT_PE_1_16_200(422, Bedrock_v422.V422_CODEC, 10);

    private final int protocol;
    private final int raknetVersion;

    private final BedrockPacketCodec defaultCodec;
    private BedrockCodec bedrockCodec;


    ProtocolVersion(int protocol, BedrockPacketCodec codec) {
        this(protocol, codec, ProtocolConstants.DEFAULT_RAKNET_VER);
    }

    ProtocolVersion(int protocol, BedrockPacketCodec codec, int raknetVersion) {
        this.protocol = protocol;
        this.defaultCodec = codec;
        this.raknetVersion = raknetVersion;
    }

    public int getProtocol() {
        return this.protocol;
    }

    public int getRaknetVersion() {
        return this.raknetVersion;
    }

    public BedrockPacketCodec getDefaultCodec() {
        return this.defaultCodec;
    }

    public BedrockPacketCodec getCodec() {
        return this.bedrockCodec == null ? this.defaultCodec : this.bedrockCodec.getPacketCodec();
    }

    public void setBedrockCodec(BedrockCodec bedrockCodec) {
        this.bedrockCodec = bedrockCodec;
    }
}
