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

package pe.waterdog.event.defaults;

import pe.waterdog.command.CommandSender;
import pe.waterdog.event.CancellableEvent;
import pe.waterdog.event.Event;

/**
 * Called whenever a command is called right before a player dispatches a proxy command.
 * Cancelling it will lead to it not executing the command.
 * Can be used to restrict access to command.
 */
public class DispatchCommandEvent extends Event implements CancellableEvent {

    private final CommandSender sender;
    private final String command;

    public DispatchCommandEvent(CommandSender sender, String command){
        this.sender = sender;
        this.command = command;
    }

    public CommandSender getSender() {
        return this.sender;
    }

    public String getCommand() {
        return this.command;
    }
}
