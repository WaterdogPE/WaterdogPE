/*
 * Copyright 2021 WaterdogTEAM
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pe.waterdog.network.downstream;

import com.nukkitx.protocol.bedrock.handler.BedrockPacketHandler;
import com.nukkitx.protocol.bedrock.packet.ChunkRadiusUpdatedPacket;
import pe.waterdog.player.ProxiedPlayer;

public abstract class AbstractDownstreamHandler implements BedrockPacketHandler {

    protected final ProxiedPlayer player;

    public AbstractDownstreamHandler(ProxiedPlayer player) {
        this.player = player;
    }

    @Override
    public boolean handle(ChunkRadiusUpdatedPacket packet) {
        this.player.getRewriteData().getChunkRadius().setRadius(packet.getRadius());
        return false;
    }
}
