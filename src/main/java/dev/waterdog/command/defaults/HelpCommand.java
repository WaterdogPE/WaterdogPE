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
import dev.waterdog.command.CommandMap;
import dev.waterdog.command.CommandSender;
import dev.waterdog.command.CommandSettings;
import dev.waterdog.utils.types.TranslationContainer;

import java.util.Map;
import java.util.TreeMap;

public class HelpCommand extends Command {

    public HelpCommand() {
        super("wdhelp", CommandSettings.builder()
                .setDescription("waterdog.command.help.description")
                .setUsageMessage("waterdog.command.help.usage")
                .setPermission("waterdog.command.help.permission")
                .build());
    }

    @Override
    public boolean onExecute(CommandSender sender, String alias, String[] args) {
        int pageNumber = 1;
        if (sender.isPlayer()) {
            if (args.length >= 1) {
                try {
                    pageNumber = Integer.parseInt(args[0]);
                    if (pageNumber < 1) pageNumber = 1;
                } catch (NumberFormatException e) {
                }
            }
        } else {
            pageNumber = Integer.MAX_VALUE;
        }

        CommandMap commandMap = sender.getProxy().getCommandMap();
        Map<String, Command> commands = new TreeMap<>();
        for (Command command : commandMap.getCommands().values()) {
            if (sender.hasPermission(command.getPermission())) {
                commands.put(command.getName(), command);
            }
        }

        int pageHeight = 8;
        int pages = commands.size() % pageHeight == 0 ? commands.size() / pageHeight : commands.size() / pageHeight + 1;
        pageNumber = Math.min(pageNumber, pages);
        if (pageNumber < 1) {
            pageNumber = 1;
        }

        sender.sendMessage(new TranslationContainer("waterdog.command.help.format", String.valueOf(pageNumber), String.valueOf(pages)));

        int i = 1;
        for (Command command : commands.values()) {
            if (i >= (pageNumber - 1) * pageHeight + 1 && i <= Math.min(commands.size(), pageNumber * pageHeight)) {
                sender.sendMessage("§6" + commandMap.getCommandPrefix() + command.getName() + ": §r" + command.getDescription());
            }
            i++;
        }
        return true;
    }
}
