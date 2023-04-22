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

package dev.waterdog.waterdogpe.network.protocol.rewrite;

import org.cloudburstmc.protocol.bedrock.packet.*;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import org.cloudburstmc.protocol.common.PacketSignal;

public class BlockMapSimple extends BlockMap {

    /*
     * We won't currently benefit from implementing runtimeId rewrite because since 1.16.100
     * client defines the block palette and block states. Servers are excepted to match the client palette.
     */

    public BlockMapSimple(ProxiedPlayer player) {
        super(player);
    }

    @Override
    public PacketSignal handle(LevelChunkPacket packet) {
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(UpdateBlockPacket packet) {
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(LevelEventPacket packet) {
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(LevelSoundEventPacket packet) {
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(AddEntityPacket packet) {
        return PacketSignal.UNHANDLED;
    }
}
