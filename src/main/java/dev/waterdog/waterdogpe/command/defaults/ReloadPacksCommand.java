/*
 * Copyright 2022 WaterdogTEAM
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

import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.command.Command;
import dev.waterdog.waterdogpe.command.CommandSender;
import dev.waterdog.waterdogpe.command.CommandSettings;

public class ReloadPacksCommand extends Command {

    public ReloadPacksCommand() {
        super("wdreloadpacks", CommandSettings.builder()
                .setDescription("waterdog.command.reloadpacks.description")
                .setPermission("waterdog.command.reloadpacks.permission")
                .setUsageMessage("waterdog.command.reloadpacks.usage").build());
    }

    @Override
    public boolean onExecute(CommandSender sender, String alias, String[] args) {
        ProxyServer proxy = ProxyServer.getInstance();
        if (!proxy.getConfiguration().enableResourcePacks()) {
            sender.sendMessage("§cResource packs are disabled in the config.");
            return true;
        }

        sender.sendMessage("§eReloading resource packs, this may take a moment if packs are downloaded from a CDN..");
        // Downloading may block, so keep it off the caller's thread; packs are swapped in atomically.
        proxy.getScheduler().scheduleAsync(() -> {
            try {
                proxy.reloadPackManager();
                sender.sendMessage("§aResource packs reloaded. New players will receive the updated packs.");
            } catch (Exception e) {
                sender.sendMessage("§cFailed to reload resource packs: " + e.getMessage());
                proxy.getLogger().error("Failed to reload resource packs", e);
            }
        });
        return true;
    }
}
