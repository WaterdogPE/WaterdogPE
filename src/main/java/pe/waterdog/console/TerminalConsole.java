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

package pe.waterdog.console;

import net.minecrell.terminalconsole.SimpleTerminalConsole;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import pe.waterdog.ProxyServer;
import pe.waterdog.command.CommandSender;

public class TerminalConsole extends SimpleTerminalConsole {

    private final ProxyServer proxy;
    private final ConsoleThread consoleThread;

    public TerminalConsole(ProxyServer proxy) {
        this.proxy = proxy;
        this.consoleThread = new ConsoleThread(this);
    }

    @Override
    protected void runCommand(String command) {
        CommandSender console = this.proxy.getConsoleSender();
        this.proxy.getScheduler().scheduleTask(() -> this.proxy.dispatchCommand(console, command), false);
    }

    @Override
    protected LineReader buildReader(LineReaderBuilder builder) {
        builder.completer(new CommandCompleter(this.proxy));
        builder.appName("WaterdogPE");
        builder.option(LineReader.Option.HISTORY_BEEP, false);
        builder.option(LineReader.Option.HISTORY_IGNORE_DUPS, true);
        builder.option(LineReader.Option.HISTORY_IGNORE_SPACE, true);
        return super.buildReader(builder);
    }

    @Override
    protected void shutdown() {
        ProxyServer.getInstance().shutdown();
    }

    @Override
    protected boolean isRunning() {
        return this.proxy.isRunning();
    }

    public ConsoleThread getConsoleThread() {
        return this.consoleThread;
    }
}
