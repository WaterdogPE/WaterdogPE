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

import jline.console.ConsoleReader;
import jline.console.CursorBuffer;

import java.io.IOException;

public class CommandReader extends Thread {

    public static CommandReader instance;
    private ConsoleReader reader;
    private CursorBuffer stashed;

    private boolean running = true;

    public CommandReader() {
        if (instance != null) {
            throw new RuntimeException("Command Reader is already exist");
        }
        try {
            this.reader = new ConsoleReader();
            reader.setPrompt("> ");
            instance = this;
        } catch (IOException e) {
            //Server.getInstance().getLogger().error("Unable to start Console Reader", e); TODO
        }
        this.setName("WaterdogPE-console");
    }

    public static CommandReader getInstance() {
        return instance;
    }

    public String readLine() {
        try {
            reader.resetPromptLine("", "", 0);
            return this.reader.readLine("> ");
        } catch (IOException e) {
            //Server.getInstance().getLogger().logException(e); TODO
            return "";
        }
    }

    public void run() {
        long lastLine = System.currentTimeMillis();

        while (this.running) {
            /*if (Server.getInstance().getConsoleSender() == null || Server.getInstance().getPluginManager() == null) {
                continue;
            }*/

            String line = readLine();

            if (line != null && !line.trim().equals("")) {
                /*try {
                    ServerCommandEvent event = new ServerCommandEvent(Server.getInstance().getConsoleSender(), line);
                    Server.getInstance().getPluginManager().callEvent(event);
                    if (!event.isCancelled()) {
                        Server.getInstance().dispatchCommand(event.getSender(), event.getCommand());
                    }
                } catch (Exception e) {
                    Server.getInstance().getLogger().logException(e);
                }*/
                lastLine = System.currentTimeMillis();
                return;

            }

            if (System.currentTimeMillis() - lastLine <= 1) {
                try {
                    sleep(40);
                } catch (InterruptedException e) {
                    //Server.getInstance().getLogger().logException(e); TODO
                }
            }
            lastLine = System.currentTimeMillis();
        }
    }

    public void shutdown() {
        this.running = false;
    }

    public synchronized void stashLine() {
        this.stashed = reader.getCursorBuffer().copy();
        try {
            reader.getOutput().write("\u001b[1G\u001b[K");
            reader.flush();
        } catch (IOException e) {
            // ignore
        }
    }

    public synchronized void unstashLine() {
        try {
            reader.resetPromptLine("> ", this.stashed.toString(), this.stashed.cursor);
        } catch (IOException e) {
            // ignore
        }
    }

    public void removePromptLine() {
        try {
            reader.resetPromptLine("", "", 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
