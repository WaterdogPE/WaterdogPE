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

package dev.waterdog.waterdogpe.event.defaults;

import dev.waterdog.waterdogpe.command.CommandSender;
import dev.waterdog.waterdogpe.event.CancellableEvent;
import dev.waterdog.waterdogpe.event.Event;

/**
 * Called whenever a command is called right before a player dispatches a proxy command.
 * Cancelling it will lead to it not executing the command.
 * Can be used to restrict access to command.
 */
public class DispatchCommandEvent extends Event implements CancellableEvent {

    private final CommandSender sender;
    private final String command;
    private final String[] args;

    public DispatchCommandEvent(CommandSender sender, String command, String[] args) {
        this.sender = sender;
        this.command = command;
        this.args = args;
    }

    public CommandSender getSender() {
        return this.sender;
    }

    public String getCommand() {
        return this.command;
    }

    public String[] getArgs() {
        return this.args;
    }
}
