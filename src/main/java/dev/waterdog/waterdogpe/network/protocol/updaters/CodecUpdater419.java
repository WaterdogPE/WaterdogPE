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

package dev.waterdog.waterdogpe.network.protocol.updaters;

import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.packet.UpdateBlockPacket;

public class CodecUpdater419 implements ProtocolCodecUpdater {

    @Override
    public BedrockCodec.Builder updateCodec(BedrockCodec.Builder builder, BedrockCodec baseCodec) {
        // Since this version block palettes are client authoritative,
        // which means we don't need to handle this anymore
        // However, we cannot deregister LevelChunkPacket as we are sending it,
        // We might consider implementing different upstream and downstream codecs in the future
        // builder.deregisterPacket(LevelChunkPacket.class);
        builder.deregisterPacket(UpdateBlockPacket.class);
        return builder;
    }

    @Override
    public int getRequiredVersion() {
        return 419;
    }
}
