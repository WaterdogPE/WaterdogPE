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

package dev.waterdog.waterdogpe.command;

import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.utils.types.TextContainer;
import dev.waterdog.waterdogpe.utils.types.TranslationContainer;

public class ConsoleCommandSender implements CommandSender {

    private final ProxyServer proxy;

    public ConsoleCommandSender(ProxyServer proxy) {
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
        for (String line : message.trim().split("\n")) {
            this.proxy.getLogger().info(line);
        }
    }

    @Override
    public void sendMessage(TextContainer message) {
        String msg;
        if (message instanceof TranslationContainer) {
            msg = ((TranslationContainer) message).getTranslated();
        } else {
            msg = message.getMessage();
        }
        this.sendMessage(msg);
    }

    @Override
    public ProxyServer getProxy() {
        return this.proxy;
    }
}
