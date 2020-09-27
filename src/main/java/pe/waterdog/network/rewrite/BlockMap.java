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

package pe.waterdog.network.rewrite;

import com.nukkitx.protocol.bedrock.BedrockPacket;
import com.nukkitx.protocol.bedrock.data.LevelEventType;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import com.nukkitx.protocol.bedrock.handler.BedrockPacketHandler;
import com.nukkitx.protocol.bedrock.packet.*;
import pe.waterdog.player.ProxiedPlayer;

public class BlockMap implements BedrockPacketHandler {

    private final ProxiedPlayer player;
    private final RewriteData rewrite;

    public BlockMap(ProxiedPlayer player){
        this.player = player;
        this.rewrite = player.getRewriteData();
    }

    public BlockPaletteRewrite getPaletteRewrite() {
        return this.rewrite.getPaletteRewrite();
    }

    public boolean doRewrite(BedrockPacket packet){
        return this.player.canRewrite() && packet.handle(this);
    }

    @Override
    public boolean handle(LevelChunkPacket packet) {
        //TODO:
        return true;
    }

    @Override
    public boolean handle(UpdateBlockPacket packet) {
        int id = packet.getRuntimeId();
        packet.setRuntimeId(this.getPaletteRewrite().map(id));
        return true;
    }

    @Override
    public boolean handle(LevelEventPacket packet) {
        LevelEventType type = packet.getType();

        //TODO: what is LEVEL_EVENT_EVENT_PARTICLE_PUNCH_BLOCK
        switch (type){
            case PARTICLE_TERRAIN:
            case PARTICLE_DESTROY_BLOCK:
                break;
            default:
                return false;
        }

        int data = packet.getData();
        int high = data & 0xFFFF0000;
        int blockID = this.getPaletteRewrite().map(data & 0xFFFF) & 0xFFFF;
        packet.setData(high | blockID);
        return true;
    }

    @Override
    public boolean handle(LevelSoundEventPacket packet) {
        if (packet.getSound() != SoundEvent.PLACE){
            return false;
        }

        int data = packet.getExtraData();
        packet.setExtraData(this.getPaletteRewrite().map(data));
        return true;
    }

    @Override
    public boolean handle(AddEntityPacket packet) {
        //TODO: entity spawn for movable blocks
        return false;
    }
}
