/*
 * Copyright 2026 WaterdogTEAM
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

import dev.waterdog.waterdogpe.command.Command;
import dev.waterdog.waterdogpe.command.CommandSender;
import dev.waterdog.waterdogpe.command.CommandSettings;
import dev.waterdog.waterdogpe.network.connection.ConnectionDiagnostics;
import dev.waterdog.waterdogpe.network.connection.client.ClientConnection;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import org.cloudburstmc.protocol.bedrock.data.command.CommandParam;
import org.cloudburstmc.protocol.bedrock.data.command.CommandOverloadData;
import org.cloudburstmc.protocol.bedrock.data.command.CommandParamData;

import java.util.List;

/**
 * Tells a player what their connection runs over, because the client shows nothing about it.
 */
public class MeCommand extends Command {

    public MeCommand() {
        super("wdme", CommandSettings.builder()
                .setDescription("waterdog.command.me.description")
                .setUsageMessage("waterdog.command.me.usage")
                .setPermission("waterdog.command.me.permission")
                .build());
    }

    @Override
    public boolean onExecute(CommandSender sender, String alias, String[] args) {
        ProxiedPlayer player;
        if (sender.isPlayer() && args.length < 1) {
            player = (ProxiedPlayer) sender;
        } else {
            if (args.length < 1) {
                return false;
            }
            if (!sender.hasPermission("waterdog.command.me.permission.other")) {
                sender.sendMessage("§cYou don't have the permission to inspect other players.");
                return true;
            }
            player = sender.getProxy().getPlayer(args[0]);
            if (player == null) {
                sender.sendMessage("§cPlayer not found!");
                return true;
            }
        }

        boolean addresses = !sender.isPlayer();

        StringBuilder sb = new StringBuilder();
        sb.append("§b--- §3Connection of ").append(player.getName()).append(" §b---\n");
        append(sb, "Client to proxy", ConnectionDiagnostics.upstream(player, addresses));

        ClientConnection downstream = player.getDownstreamConnection();
        if (downstream != null) {
            append(sb, "Proxy to server", ConnectionDiagnostics.downstream(downstream, player.getProtocol(), addresses));
        } else {
            sb.append("§3Proxy to server: §cnot connected\n");
        }
        ClientConnection pending = player.getPendingConnection();
        if (pending != null && pending != downstream) {
            append(sb, "Connecting to", ConnectionDiagnostics.downstream(pending, player.getProtocol(), addresses));
        }
        pings(sb, player.getPing(), downstream == null ? 0 : downstream.getPing());
        sender.sendMessage(sb.toString().stripTrailing());
        return true;
    }

    /**
     * Both legs side by side, and their sum, which is the closest thing to the latency the
     * player feels. Shown as soon as either leg has a value.
     */
    private static void pings(StringBuilder sb, long upstream, long downstream) {
        if (upstream <= 0 && downstream <= 0) {
            return;
        }
        sb.append("§ePing\n");
        sb.append("§3Player to proxy: §b").append(ConnectionDiagnostics.ping(upstream)).append('\n');
        sb.append("§3Proxy to server: §b").append(ConnectionDiagnostics.ping(downstream)).append('\n');
        if (upstream > 0 && downstream > 0) {
            sb.append("§3End to end: §b").append(upstream + downstream).append(" ms §7(through the proxy)\n");
        }
    }

    private static void append(StringBuilder sb, String section, List<ConnectionDiagnostics.Line> lines) {
        sb.append("§e").append(section).append('\n');
        for (ConnectionDiagnostics.Line line : lines) {
            sb.append("§3").append(line.label()).append(": §b").append(line.value());
            if (line.note() != null) {
                sb.append(" §7(").append(line.note()).append(')');
            }
            sb.append('\n');
        }
    }

    @Override
    protected CommandOverloadData[] buildCommandOverloads() {
        CommandParamData player = new CommandParamData();
        player.setName("player");
        player.setOptional(true);
        player.setType(CommandParam.TARGET);
        return new CommandOverloadData[]{new CommandOverloadData(false, new CommandParamData[]{player})};
    }
}
