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

package pe.waterdog.command.defaults;

import com.nukkitx.protocol.bedrock.data.command.CommandData;
import com.nukkitx.protocol.bedrock.data.command.CommandParamData;
import com.nukkitx.protocol.bedrock.data.command.CommandParamType;
import pe.waterdog.command.Command;
import pe.waterdog.command.CommandSender;
import pe.waterdog.command.CommandSettings;
import pe.waterdog.network.ServerInfo;
import pe.waterdog.player.ProxiedPlayer;
import pe.waterdog.utils.types.TextContainer;

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
                new CommandParamData("destination", false, null, CommandParamType.TEXT, null, Collections.emptyList()),
                new CommandParamData("target", true, null, CommandParamType.TEXT, null, Collections.emptyList())
        }};
        return new CommandData(this.getName(), this.getDescription(), Collections.emptyList(), (byte) 0, null, parameterData);
    }
}
