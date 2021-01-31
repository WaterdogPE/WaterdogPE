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

import dev.waterdog.ProxyServer;
import dev.waterdog.command.Command;
import dev.waterdog.command.CommandSender;
import dev.waterdog.command.CommandSettings;

public class EndCommand extends Command {

    public EndCommand() {
        super("end", CommandSettings.builder()
                .setDescription("waterdog.command.end.description")
                .setPermission("waterdog.command.end.permission")
                .setUsageMessage("waterdog.command.end.usage").build());
    }

    @Override
    public boolean onExecute(CommandSender sender, String alias, String[] args) {
        sender.sendMessage("§aShutting down the proxy instance..");
        ProxyServer.getInstance().shutdown();
        return true;
    }
}
