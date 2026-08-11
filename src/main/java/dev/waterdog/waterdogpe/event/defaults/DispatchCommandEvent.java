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

import dev.waterdog.waterdogpe.command.Command;
import dev.waterdog.waterdogpe.command.CommandSender;
import dev.waterdog.waterdogpe.event.CancellableEvent;
import dev.waterdog.waterdogpe.event.Event;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * Called before a command sent by any {@link CommandSender} is executed, including commands that
 * are not registered on the proxy and are only meant for the downstream server. Those have no
 * resolved command.
 * <p>
 * Canceling and consuming are separate. Canceling skips the proxy side execution, while the
 * consume state decides whether the command is passed to the downstream server. Console commands
 * have no downstream, so the consume state has no effect on them.
 * <p>
 * To restrict access to a command, cancel the event and set {@link ConsumeState#CONSUME}, else the
 * downstream server still receives it.
 */
@Getter
public class DispatchCommandEvent extends Event implements CancellableEvent {

    private final CommandSender sender;
    /**
     * The name or alias the command was called by, without the command prefix.
     */
    private final String alias;
    /**
     * The arguments following the command name, parsed quote aware when the resolved proxy command
     * asks for it in its {@link dev.waterdog.waterdogpe.command.CommandSettings}.
     */
    private final String[] args;
    /**
     * The command registered under the used name or alias, null when the proxy does not know it.
     */
    private final Command resolvedCommand;
    @Setter
    @NonNull
    private ConsumeState consumeState = ConsumeState.DEFAULT;

    public DispatchCommandEvent(CommandSender sender, String alias, String[] args, Command resolvedCommand) {
        this.sender = sender;
        this.alias = alias;
        this.args = args;
        this.resolvedCommand = resolvedCommand;
    }

    /**
     * @deprecated the name is ambiguous now that the resolved command is exposed, use
     * {@code getAlias()} for the name or {@code getResolvedCommand()} for the command itself.
     */
    @Deprecated
    public String getCommand() {
        return alias;
    }

    /**
     * Resolves the consume state against the outcome of the dispatch.
     *
     * @param defaultState whether the command would be consumed without plugin interaction.
     * @return whether the command must not be passed to the downstream server.
     */
    public boolean isConsumed(boolean defaultState) {
        return switch (this.consumeState) {
            case CONSUME -> true;
            case PASS_THROUGH -> false;
            case DEFAULT -> defaultState;
        };
    }

    /**
     * Decides whether the command is passed to the downstream server.
     */
    public enum ConsumeState {
        /**
         * Consumed only when a proxy command handled it and the event was not canceled.
         */
        DEFAULT,
        /**
         * Never passed to the downstream server, even when no proxy command handled it.
         * Use this to handle a command inside the plugin without the downstream server seeing it.
         */
        CONSUME,
        /**
         * Always passed to the downstream server, even when a proxy command handled it.
         * The downstream server may run its own command of the same name on top of the proxy one.
         */
        PASS_THROUGH
    }

}
