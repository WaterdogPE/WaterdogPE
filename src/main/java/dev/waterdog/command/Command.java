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

package dev.waterdog.command;

import com.nukkitx.protocol.bedrock.data.command.CommandData;
import com.nukkitx.protocol.bedrock.data.command.CommandParamData;
import com.nukkitx.protocol.bedrock.data.command.CommandParamType;

import java.util.Collections;

/**
 * Base class for proxy commands
 */
public abstract class Command {

    /**
     * The name of the command
     */
    private final String name;

    /**
     * The command settings assigned to it
     */
    private final CommandSettings settings;

    private final CommandData data;

    public Command(String name, CommandSettings settings) {
        this.name = name;
        this.settings = settings;
        this.data = this.craftNetwork();
    }

    public abstract boolean onExecute(CommandSender sender, String alias, String[] args);

    public CommandSettings getSettings() {
        return this.settings;
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.settings.getDescription();
    }

    public String getUsageMessage() {
        return this.settings.getUsageMessage();
    }

    public String getPermissionMessage() {
        return this.settings.getPermissionMessage();
    }

    public CommandData getData() {
        return this.data;
    }

    public String getPermission() {
        return this.settings.getPermission();
    }

    public String[] getAliases() {
        return this.settings.getAliases();
    }


    public CommandData craftNetwork() {
        CommandParamData[][] parameterData = new CommandParamData[][]{{
                new CommandParamData(this.name, true, null, CommandParamType.TEXT, null, Collections.emptyList())
        }};
        return new CommandData(this.name, this.getDescription(), Collections.emptyList(), (byte) 0, null, parameterData);
    }
}
