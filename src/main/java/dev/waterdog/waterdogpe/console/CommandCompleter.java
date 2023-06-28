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

package dev.waterdog.waterdogpe.console;

import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.command.CommandMap;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class CommandCompleter implements Completer {

    private final ProxyServer proxy;

    public CommandCompleter(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Override
    public void complete(LineReader lineReader, ParsedLine parsedLine, List<Candidate> candidates) {
        if (parsedLine.wordIndex() == 0) {
            if (parsedLine.word().isEmpty()) {
                this.addOptions(command -> candidates.add(new Candidate(command)));
                return;
            }

            List<String> commands = new ArrayList<>();
            this.addOptions(commands::add);
            for (String command : commands) {
                if (command.startsWith(parsedLine.word())) {
                    candidates.add(new Candidate(command));
                }
            }
            return;
        }

        if (parsedLine.wordIndex() > 1 && !parsedLine.word().isEmpty()) {
            String world = parsedLine.word();
            for (ProxiedPlayer player : this.proxy.getPlayers().values()) {
                if (player.getName().toLowerCase().startsWith(world)) {
                    candidates.add(new Candidate(player.getName()));
                }

            }
        }
    }

    private void addOptions(Consumer<String> commandConsumer) {
        CommandMap commandMap = this.proxy.getCommandMap();
        for (String command : commandMap.getCommands().keySet()) {
            commandConsumer.accept(command);
        }
    }
}
