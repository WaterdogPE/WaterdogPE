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

public interface CommandMap {

    boolean registerCommand(String name, Command command);
    boolean registerAlias(String name, Command command);
    boolean unregisterCommand(String name);

    boolean isRegistered(String name);

    /**
     * Returns true if command can be handled by this command map.
     */
    boolean handleMessage(CommandSender sender, String message);

    /**
     * Returns true if command was handled.
     * WARNING: Will return true even if command was handled but thrown exception!
     */
    boolean handleCommand(CommandSender sender, String command, String[] args);

    String getCommandPrefix();
    Object2ObjectMap<String, Command> getCommands();
}
