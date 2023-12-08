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

package dev.waterdog.waterdogpe.command;

import org.cloudburstmc.protocol.bedrock.data.command.*;

import java.util.*;

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

    private CommandData commandData;

    public Command(String name) {
        this(name, CommandSettings.empty());
    }

    public Command(String name, CommandSettings settings) {
        this.name = name;
        this.settings = settings;
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

    public CommandData getCommandData() {
        if (this.commandData == null) {
            this.commandData = this.buildNetworkData();
        }
        return this.commandData;
    }

    public String getPermission() {
        return this.settings.getPermission();
    }

    public Set<String> getAliases() {
        return this.settings.getAliases();
    }

    private CommandData buildNetworkData() {
        // Create command aliases
        Map<String, Set<CommandEnumConstraint>> aliases = new LinkedHashMap<>();
        aliases.put(this.name, EnumSet.of(CommandEnumConstraint.ALLOW_ALIASES));

        for (String alias : this.settings.getAliases()) {
            aliases.put(alias, EnumSet.of(CommandEnumConstraint.ALLOW_ALIASES));
        }

        // Build command parameters
        CommandOverloadData[] overloads = this.buildCommandOverloads();

        return new CommandData(this.name,
                this.getDescription(),
                Collections.emptySet(),
                CommandPermission.ANY,
                new CommandEnumData(this.name + "_aliases", aliases, false),
                Collections.emptyList(),
                overloads);
    }

    protected CommandOverloadData[] buildCommandOverloads() {
        CommandParamData simpleData = new CommandParamData();
        simpleData.setName(this.name);
        simpleData.setOptional(true);
        simpleData.setType(CommandParam.TEXT);
        return new CommandOverloadData[]{new CommandOverloadData(false, new CommandParamData[]{simpleData})};
    }
}
