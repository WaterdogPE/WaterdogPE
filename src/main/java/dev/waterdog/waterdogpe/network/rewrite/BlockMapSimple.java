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

package dev.waterdog.waterdogpe.network.rewrite;

import com.nukkitx.protocol.bedrock.packet.*;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;

public class BlockMapSimple extends BlockMap {

    /*
     * TODO: since 1.16.220 item packets contain baked-in blockRuntimeId
     * We won't currently benefit from implementing runtimeId rewrite because since 1.16.100
     * client defines the block palette and block states. Servers are excepted to match the client palette.
     * However there might occur some small differences which might be reason to enable rewrite. Because this
     * does happen only when transferring between different downstream software or with modded servers,
     * it will be implemented with upcoming refactor.
     */

    public BlockMapSimple(ProxiedPlayer player) {
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
