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

package pe.waterdog.command;

public class CommandSettings {

    private final String usageMessage;
    private final String description;

    private final String permission;
    private final String permissionMessage;

    private final String[] aliases;

    private CommandSettings(String usageMessage, String description, String permission, String[] aliases, String permissionMessage) {
        this.usageMessage = usageMessage;
        this.description = description;
        this.permission = permission;
        this.permissionMessage = permissionMessage;
        this.aliases = aliases;
    }

    public static Builder builder(){
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

    public String[] getAliases() {
        return this.aliases;
    }

    public static class Builder {
        private String usageMessage = "";
        private String description = null;
        private String permission = "";
        private String permissionMessage = "waterdog.command.permission.failed";
        private String[] aliases = new String[0];

        public CommandSettings build(){
            return new CommandSettings(
                    this.usageMessage,
                    this.description == null? this.usageMessage : this.description,
                    this.permission,
                    this.aliases,
                    this.permissionMessage
            );
        }

        public Builder setUsageMessage(String usageMessage) {
            this.usageMessage = usageMessage;
            return this;
        }

        public String getUsageMessage() {
            return this.usageMessage;
        }

        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        public String getDescription() {
            return this.description;
        }

        public Builder setPermission(String permission) {
            this.permission = permission;
            return this;
        }

        public String getPermission() {
            return this.permission;
        }

        public Builder setPermissionMessage(String permissionMessage) {
            this.permissionMessage = permissionMessage;
            return this;
        }

        public String getPermissionMessage() {
            return this.permissionMessage;
        }

        public Builder setAliases(String[] aliases) {
            this.aliases = aliases;
            return this;
        }

        public String[] getAliases() {
            return this.aliases;
        }
    }
}
