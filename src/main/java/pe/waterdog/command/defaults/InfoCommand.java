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

import pe.waterdog.VersionInfo;
import pe.waterdog.command.Command;
import pe.waterdog.command.CommandSender;
import pe.waterdog.command.CommandSettings;

public class InfoCommand extends Command {

    public InfoCommand() {
        super("wdinfo", CommandSettings.builder()
                .setDescription("waterdog.command.info.description")
                .setUsageMessage("waterdog.command.info.usage")
                .setPermission("waterdog.command.info.permission")
                .build());
    }

    @Override
    public boolean onExecute(CommandSender sender, String alias, String[] args) {
        sender.sendMessage("§bRunning WaterdogPE version §3" + VersionInfo.BASE_VERSION + "§b!\n" +
                "§3Build Version: §b" + VersionInfo.BUILD_VERSION + "\n" +
                "§3Latest Protocol: §b" + VersionInfo.LATEST_PROTOCOL_VERSION + "\n" +
                "§3Author: §b" + VersionInfo.AUTHOR + "\n" +
                "§3Developer Mode: " + (VersionInfo.IS_DEVELOPMENT ? "§cenabled" : "§adisabled"));
        return true;
    }
}
