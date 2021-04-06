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

package dev.waterdog.network.downstream;

import com.nukkitx.protocol.bedrock.handler.BedrockPacketHandler;
import com.nukkitx.protocol.bedrock.packet.AvailableCommandsPacket;
import com.nukkitx.protocol.bedrock.packet.ChunkRadiusUpdatedPacket;
import dev.waterdog.command.Command;
import dev.waterdog.player.ProxiedPlayer;

public abstract class AbstractDownstreamHandler implements BedrockPacketHandler {

    protected final ProxiedPlayer player;

    public AbstractDownstreamHandler(ProxiedPlayer player) {
        this.player = player;
    }

    @Override
    public boolean handle(AvailableCommandsPacket packet) {
        if (!this.player.getProxy().getConfiguration().injectCommands()) {
            return false;
        }
        int sizeBefore = packet.getCommands().size();

        for (Command command : this.player.getProxy().getCommandMap().getCommands().values()) {
            if (command.getPermission() == null || this.player.hasPermission(command.getPermission())) {
                packet.getCommands().add(command.getData());
            }
        }
        return packet.getCommands().size() > sizeBefore;
    }

    @Override
    public boolean handle(ChunkRadiusUpdatedPacket packet) {
        this.player.getRewriteData().getChunkRadius().setRadius(packet.getRadius());
        return false;
    }
}
