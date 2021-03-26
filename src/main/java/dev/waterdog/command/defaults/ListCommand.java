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

import dev.waterdog.command.Command;
import dev.waterdog.command.CommandSender;
import dev.waterdog.command.CommandSettings;
import dev.waterdog.network.ServerInfo;
import dev.waterdog.player.ProxiedPlayer;
import dev.waterdog.utils.types.TranslationContainer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;

public class ListCommand extends Command {

    public ListCommand() {
        super("wdlist", CommandSettings.builder()
                .setDescription("waterdog.command.list.description")
                .setUsageMessage("waterdog.command.list.usage")
                .setPermission("waterdog.command.list.permission")
                .build());
    }

    @Override
    public boolean onExecute(CommandSender sender, String alias, String[] args) {
        if (args.length >= 1) {
            ServerInfo serverInfo = sender.getProxy().getServerInfo(args[0]);
            sender.sendMessage(serverInfo == null ? "§cServer not found!" : this.buildServerList(serverInfo));
            return true;
        }

        List<ServerInfo> servers = new ArrayList<>(sender.getProxy().getServers());
        servers.sort(Comparator.comparing(ServerInfo::getServerName));

        StringBuilder builder = new StringBuilder("§aShowing all servers:\n");
        for (ServerInfo serverInfo : servers) {
            builder.append(this.buildServerList(serverInfo)).append("\n§r");
        }

        builder.append("§rTotal online players: ").append(sender.getProxy().getPlayers().size());
        sender.sendMessage(builder.toString());
        return true;
    }

    private String buildServerList(ServerInfo serverInfo) {
        StringJoiner joiner = new StringJoiner(",");
        for (ProxiedPlayer player : serverInfo.getPlayers()) {
            joiner.add(player.getName());
        }

        return new TranslationContainer("waterdog.command.list.format",
                serverInfo.getServerName(),
                String.valueOf(serverInfo.getPlayers().size()),
                joiner.toString()
        ).getTranslated();
    }
}
