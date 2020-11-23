/*
 * Copyright 2020 WaterdogTEAM
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package pe.waterdog.network.rewrite;

import com.nukkitx.protocol.bedrock.packet.*;
import pe.waterdog.player.ProxiedPlayer;

public class BlockMapModded extends BlockMap{

    public BlockMapModded(ProxiedPlayer player) {
        super(player);
    }

    @Override
    public boolean handle(LevelChunkPacket packet) {
        return false;
    }

    @Override
    public boolean handle(UpdateBlockPacket packet) {
        return false;
    }

    @Override
    public boolean handle(LevelEventPacket packet) {
        return false;
    }

    @Override
    public boolean handle(LevelSoundEventPacket packet) {
        return false;
    }

    @Override
    public boolean handle(AddEntityPacket packet) {
        return false;
    }
}
