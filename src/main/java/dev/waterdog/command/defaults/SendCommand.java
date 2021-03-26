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

package dev.waterdog.command.defaults;

import com.nukkitx.protocol.bedrock.data.command.CommandData;
import com.nukkitx.protocol.bedrock.data.command.CommandParam;
import com.nukkitx.protocol.bedrock.data.command.CommandParamData;
import com.nukkitx.protocol.bedrock.data.command.CommandParamType;
import dev.waterdog.command.Command;
import dev.waterdog.command.CommandSender;
import dev.waterdog.command.CommandSettings;
import dev.waterdog.network.ServerInfo;
import dev.waterdog.player.ProxiedPlayer;
import dev.waterdog.utils.types.TextContainer;

import java.util.Collections;

public class SendCommand extends Command {

    public SendCommand() {
        super("wdsend", CommandSettings.builder()
                .setDescription("waterdog.command.send.description")
                .setUsageMessage("waterdog.command.send.usage")
                .setPermission("waterdog.command.send.permission")
                .build());
    }

    @Override
    public boolean onExecute(CommandSender sender, String alias, String[] args) {
        if (args.length < 1 || !sender.isPlayer() && args.length < 2) {
            return false;
        }

        ServerInfo server = sender.getProxy().getServerInfo(args[0]);
        if (server == null) {
            sender.sendMessage(new TextContainer("§cServer {%0} was not found!", args[0]));
            return true;
        }

        ServerInfo targetServer;
        if (sender.isPlayer() && args.length < 2) {
            targetServer = ((ProxiedPlayer) sender).getServerInfo();
        } else {
            targetServer = sender.getProxy().getServerInfo(args[1]);
        }

        if (targetServer == null) {
            sender.sendMessage(new TextContainer("§cCould not find target server {%0}!", args[1]));
            return true;
        }

        for (ProxiedPlayer player : targetServer.getPlayers()) {
            player.connect(server);
        }
        return true;
    }

    @Override
    public CommandData craftNetwork() {
        CommandParamData[][] parameterData = new CommandParamData[][]{{
                new CommandParamData("destination", false, null, CommandParam.TEXT, null, Collections.emptyList()),
                new CommandParamData("target", true, null, CommandParam.TEXT, null, Collections.emptyList())
        }};
        return new CommandData(this.getName(), this.getDescription(), Collections.emptyList(), (byte) 0, null, parameterData);
    }
}
