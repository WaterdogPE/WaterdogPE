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

import dev.waterdog.waterdogpe.utils.types.TranslationContainer;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A container holding base information of each command
 */
public class CommandSettings {

    private static final CommandSettings EMPTY_SETTINGS = CommandSettings.builder().build();

    private final String usageMessage;
    private final String description;
    private final boolean quoteAware;

    private final String permission;
    private final String permissionMessage;

    private final Set<String> aliases;

    private CommandSettings(String usageMessage, String description, String permission, Set<String> aliases, String permissionMessage, boolean quoteAware) {
        this.usageMessage = new TranslationContainer(usageMessage).getTranslated();
        this.description = new TranslationContainer(description).getTranslated();
        this.permission = new TranslationContainer(permission).getTranslated();
        this.permissionMessage = new TranslationContainer(permissionMessage).getTranslated();
        this.quoteAware = quoteAware;
        this.aliases = aliases;
    }

    public static CommandSettings empty() {
        return EMPTY_SETTINGS;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getUsageMessage() {
        return this.usageMessage;
    }

    public String getDescription() {
        return this.description;
    }

    public String getPermission() {
        return this.permission;
    }

    public String getPermissionMessage() {
        return this.permissionMessage;
    }

    public Set<String> getAliases() {
        return this.aliases;
    }

    public boolean isQuoteAware() {
        return quoteAware;
    }

    public static class Builder {
        private String usageMessage = "";
        private String description = null;
        private String permission = "";
        private String permissionMessage = "waterdog.command.permission.failed";
        private String[] aliases = new String[0];
        private boolean quoteAware = false;

        public CommandSettings build() {
            return new CommandSettings(
                    this.usageMessage,
                    this.description == null ? this.usageMessage : this.description,
                    this.permission,
                    Collections.unmodifiableSet(new HashSet<>(Arrays.asList(this.aliases))),
                    this.permissionMessage,
                    this.quoteAware
            );
        }

        public String getUsageMessage() {
            return this.usageMessage;
        }

        public Builder setUsageMessage(String usageMessage) {
            this.usageMessage = usageMessage;
            return this;
        }

        public boolean isQuoteAware() {
            return quoteAware;
        }

        public Builder setQuoteAware(boolean quoteAware) {
            this.quoteAware = quoteAware;

            return this;
        }

        public String getDescription() {
            return this.description;
        }

        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        public String getPermission() {
            return this.permission;
        }

        public Builder setPermission(String permission) {
            this.permission = permission;
            return this;
        }

        public String getPermissionMessage() {
            return this.permissionMessage;
        }

        public Builder setPermissionMessage(String permissionMessage) {
            this.permissionMessage = permissionMessage;
            return this;
        }

        public String[] getAliases() {
            return this.aliases;
        }

        public Builder setAliases(String... aliases) {
            this.aliases = aliases;
            return this;
        }
    }
}
