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

import com.google.common.base.Preconditions;
import com.nukkitx.protocol.bedrock.BedrockPacketCodec;
import pe.waterdog.network.protocol.ProtocolVersion;

public abstract class BedrockCodec {

    private BedrockPacketCodec packetCodec;

    public BedrockCodec(){
    }

    public BedrockPacketCodec.Builder createBuilder(BedrockPacketCodec defaultCodec){
        return this.createBuilder(defaultCodec.getProtocolVersion(), defaultCodec.getRaknetProtocolVersion(), defaultCodec.getMinecraftVersion());
    }

    /**
     * Creates default builder that will be used in buildCodec() method.
     * @param protocol protocol number.
     * @param raknetVersion version number of RakNet that client uses.
     * @param minecraftVer name of version in string.
     * @return BedrockPacketCodec builder.
     */
    public BedrockPacketCodec.Builder createBuilder(int protocol, int raknetVersion, String minecraftVer){
        Preconditions.checkArgument(this.packetCodec == null, "Packet codec has been already built!");
        BedrockPacketCodec.Builder builder = BedrockPacketCodec.builder();
        builder.protocolVersion(protocol);
        builder.raknetProtocolVersion(raknetVersion);
        builder.minecraftVersion(minecraftVer);
        return builder;
    }

    /**
     * This method should be implemented in parent.
     * Some common packets may be implemented here later.
     * @param builder can be edited inside of the function. Builder is used to register or deregister packets.
     */
    public void buildCodec(BedrockPacketCodec.Builder builder){
        //Maybe later put common packets here
    }

    public abstract ProtocolVersion getProtocol();

    public void setPacketCodec(BedrockPacketCodec packetCodec) {
        Preconditions.checkNotNull(packetCodec, "New packet codec can not be null!");
        Preconditions.checkNotNull(this.packetCodec, "Packet codec can not be null!");
        this.packetCodec = packetCodec;
    }

    public BedrockPacketCodec getPacketCodec() {
        return this.packetCodec;
    }
}
