/*
 * Copyright 2022 WaterdogTEAM
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
import com.nukkitx.protocol.bedrock.packet.NetworkSettingsPacket;
import com.nukkitx.protocol.bedrock.packet.RequestNetworkSettingsPacket;
import com.nukkitx.protocol.bedrock.packet.TextPacket;
import com.nukkitx.protocol.bedrock.v554.BedrockPacketHelper_v554;
import com.nukkitx.protocol.bedrock.v554.serializer.NetworkSettingsSerializer_v554;
import com.nukkitx.protocol.bedrock.v554.serializer.RequestNetworkSettingsSerializer_v554;
import com.nukkitx.protocol.bedrock.v554.serializer.TextSerializer_v554;
import dev.waterdog.waterdogpe.network.protocol.ProtocolVersion;

public class BedrockCodec554 extends BedrockCodec545 {

    @Override
    public ProtocolVersion getProtocol() {
        return ProtocolVersion.MINECRAFT_PE_1_19_30;
    }

    @Override
    public void buildCodec(BedrockPacketCodec.Builder builder) {
        super.buildCodec(builder);
        builder.helper(BedrockPacketHelper_v554.INSTANCE);

        builder.deregisterPacket(TextPacket.class);
        builder.registerPacket(TextPacket.class, TextSerializer_v554.INSTANCE, 9);

        builder.registerPacket(NetworkSettingsPacket.class, NetworkSettingsSerializer_v554.INSTANCE, 143);
        builder.registerPacket(RequestNetworkSettingsPacket.class, RequestNetworkSettingsSerializer_v554.INSTANCE, 193);
    }
}