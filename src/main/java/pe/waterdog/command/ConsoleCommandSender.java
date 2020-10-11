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

import pe.waterdog.ProxyServer;
import pe.waterdog.logger.MainLogger;
import pe.waterdog.utils.types.TextContainer;
import pe.waterdog.utils.types.TranslationContainer;

public class ConsoleCommandSender implements CommandSender {

    private final ProxyServer proxy;

    public ConsoleCommandSender(ProxyServer proxy){
        this.proxy = proxy;
    }

    @Override
    public String getName() {
        return "Console";
    }

    @Override
    public boolean isPlayer() {
        return false;
    }

    @Override
    public boolean hasPermission(String permission) {
        return true;
    }

    @Override
    public void sendMessage(String message) {
        MainLogger logger = ProxyServer.getInstance().getLogger();
        for (String line : message.trim().split("\n")){
            logger.info(line);
        }
    }

    @Override
    public void sendMessage(TextContainer message) {
        String msg;
        if (message instanceof TranslationContainer){
            msg = ((TranslationContainer) message).getTranslated();
        }else {
            msg = message.getMessage();
        }
        this.sendMessage(msg);
    }

    @Override
    public ProxyServer getProxy() {
        return this.proxy;
    }
}
