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

package dev.waterdog.waterdogpe.command.defaults;

import com.nukkitx.protocol.bedrock.data.command.CommandData;
import com.nukkitx.protocol.bedrock.data.command.CommandParam;
import com.nukkitx.protocol.bedrock.data.command.CommandParamData;
import dev.waterdog.waterdogpe.command.Command;
import dev.waterdog.waterdogpe.command.CommandSender;
import dev.waterdog.waterdogpe.command.CommandSettings;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import dev.waterdog.waterdogpe.utils.types.TextContainer;

import java.util.Collections;

public class ServerCommand extends Command {

    public ServerCommand() {
        super("server", CommandSettings.builder()
                .setDescription("waterdog.command.server.description")
                .setUsageMessage("waterdog.command.server.usage")
                .setPermission("waterdog.command.server.permission")
                .build());
    }

    @Override
    public boolean onExecute(CommandSender sender, String alias, String[] args) {
        if (args.length < 1 || !sender.isPlayer() && args.length < 2) {
            return false;
        }

        ServerInfo server = sender.getProxy().getServerInfo(args[0]);
        if (server == null) {
            sender.sendMessage("§cServer not found!");
            return true;
        }

        ProxiedPlayer player;
        if (sender.isPlayer() && args.length < 2) {
            player = (ProxiedPlayer) sender;
        } else {
            if (!sender.hasPermission("waterdog.command.server.permission.other")) {
                sender.sendMessage(new TextContainer("§cYou don't have the permission to move other players."));
                return false;
            }

            player = sender.getProxy().getPlayer(args[1]);
            if (player == null) {
                sender.sendMessage("§cPlayer not found!");
                return true;
            }
            sender.sendMessage(new TextContainer("§aPlayer {%0} was sent to {%1} server!", player.getName(), server.getServerName()));
        }

        player.connect(server);
        return true;
    }

    @Override
    public CommandData craftNetwork() {
        CommandParamData[][] parameterData = new CommandParamData[][]{{
                new CommandParamData("server", false, null, CommandParam.TEXT, null, Collections.emptyList()),
                new CommandParamData("player", true, null, CommandParam.TARGET, null, Collections.emptyList())
        }};
        return new CommandData(this.getName(), this.getDescription(), Collections.emptyList(), (byte) 0, null, parameterData);
    }
}
