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

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import pe.waterdog.ProxyServer;
import pe.waterdog.utils.types.TranslationContainer;

public class SimpleCommandMap implements CommandMap {

    public static final String DEFAULT_PREFIX = "/";

    private final ProxyServer proxy;
    private final String commandPrefix;

    private final Object2ObjectMap<String, Command> commandsMap = new Object2ObjectOpenHashMap<>();
    private final Object2ObjectMap<String, Command> aliasesMap = new Object2ObjectOpenHashMap<>();

    public SimpleCommandMap(ProxyServer proxy, String prefix){
        this.proxy = proxy;
        this.commandPrefix = prefix;
    }

    @Override
    public boolean registerCommand(String name, Command command) {
        return this.commandsMap.putIfAbsent(name.toLowerCase(), command) == null;
    }

    @Override
    public boolean registerAlias(String name, Command command) {
        return this.aliasesMap.putIfAbsent(name.toLowerCase(), command) == null;
    }

    @Override
    public boolean unregisterCommand(String name) {
        Command command = this.commandsMap.get(name.toLowerCase());
        if (command == null) return false;

        for (String alias : command.getAliases()){
            this.aliasesMap.remove(alias.toLowerCase());
        }
        return true;
    }

    @Override
    public boolean isRegistered(String name) {
        return this.commandsMap.containsKey(name.toLowerCase());
    }

    @Override
    public boolean handleMessage(CommandSender sender, String message) {
        return message.startsWith(this.commandPrefix);
    }

    @Override
    public boolean handleCommand(CommandSender sender, String commandName, String[] args) {
        Command command = this.commandsMap.get(commandName.toLowerCase());
        if (command != null){
            this.execute(command, sender, null, args);
            return true;
        }

        Command aliasCommand = this.aliasesMap.get(commandName.toLowerCase());
        if (aliasCommand != null){
            this.execute(aliasCommand, sender, commandName, args);
            return true;
        }
        sender.sendMessage(new TranslationContainer("waterdog.command.unknown"));
        return false;
    }

    private void execute(Command command, CommandSender sender, String alias, String[] args){
        boolean permission = sender.hasPermission(command.getPermission());
        if (!permission){
            sender.sendMessage(new TranslationContainer(command.getPermissionMessage()));
            return;
        }

        try {
            boolean success = command.onExecute(sender, alias, args);
            if (!success){
                sender.sendMessage(new TranslationContainer(command.getUsageMessage()));
            }
        }catch (Exception e){
            this.proxy.getLogger().error("Error appeared while processing command!", e);
        }
    }

    @Override
    public String getCommandPrefix() {
        return this.commandPrefix;
    }

    @Override
    public Object2ObjectMap<String, Command> getCommands() {
        return this.commandsMap;
    }
}
