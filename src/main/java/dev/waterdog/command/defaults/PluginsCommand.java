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
import dev.waterdog.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PluginsCommand extends Command {

    public PluginsCommand() {
        super("wdplugins", CommandSettings.builder()
                .setDescription("waterdog.command.plugins.description")
                .setUsageMessage("waterdog.command.plugins.usage")
                .setPermission("waterdog.command.plugins.permission")
                .build());
    }

    @Override
    public boolean onExecute(CommandSender sender, String alias, String[] args) {
        List<String> pluginmap = new ArrayList<>();
        Collection<Plugin> installedplugins = ProxyServer.getInstance().getPluginManager().getPlugins();
        for (Plugin p : installedplugins) {
            pluginmap.add(checkPlugin(p));
        }
        sender.sendMessage("§eWaterdogPE Plugins [" +  installedplugins.size() + "]: " + String.join("§7, ", pluginmap));
        return true;
    }

    private String checkPlugin(Plugin targetplugin){
        if(targetplugin.isEnabled()){
            return "§a" + targetplugin.getName() + " : " + targetplugin.getDescription().getVersion() + " by " + targetplugin.getDescription().getAuthor();
        }
        return "§c" + targetplugin.getName() + " : " + targetplugin.getDescription().getVersion() + " by " + targetplugin.getDescription().getAuthor();
    }
}
