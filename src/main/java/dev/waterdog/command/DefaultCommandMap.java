/*
 * Copyright 2021 WaterdogTEAM
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

package dev.waterdog.command;

import dev.waterdog.ProxyServer;
import dev.waterdog.command.defaults.*;

public class DefaultCommandMap extends SimpleCommandMap {

    public DefaultCommandMap(ProxyServer proxy, String prefix) {
        super(proxy, prefix);
        this.registerDefaults();
    }

    public void registerDefaults() {
        this.registerCommand("wdhelp", new HelpCommand());
        this.registerCommand("wdlist", new ListCommand());
        this.registerCommand("wdinfo", new InfoCommand());
        this.registerCommand("server", new ServerCommand());
        this.registerCommand("wdsend", new SendCommand());
        this.registerCommand("wdplugins", new PluginsCommand());
        this.registerCommand("end", new EndCommand());
    }
}
