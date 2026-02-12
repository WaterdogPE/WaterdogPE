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
import dev.waterdog.waterdogpe.plugin.Plugin;
import dev.waterdog.waterdogpe.utils.types.TranslationContainer;

import java.util.Map;

public class PluginsCommand extends Command {
    public PluginsCommand() {
        super("wdplugins", CommandSettings.builder().
                setDescription("waterdog.command.plugins.description")
                .setUsageMessage("waterdog.command.plugins.usage")
                .setPermission("waterdog.command.plugins.permission")
                .setAliases("wdpl").build());
    }

    @Override
    public boolean onExecute(CommandSender sender, String alias, String[] args) {
        Map<String, Plugin> plugins = sender.getProxy().getPluginManager().getPluginMap();
        StringBuilder list = new StringBuilder();
        for (Plugin plugin: plugins.values()) {
            if (list.length() > 0) {
                list.append("§f, ");
            }
            list.append(plugin.isEnabled() ? "§a" : "§c");
            list.append(plugin.getDescription().getName()).append(" v").append(plugin.getDescription().getVersion());
        }

        sender.sendMessage(new TranslationContainer("waterdog.command.plugins.format", String.valueOf(plugins.size()), String.valueOf(list)));
        return true;
    }
}