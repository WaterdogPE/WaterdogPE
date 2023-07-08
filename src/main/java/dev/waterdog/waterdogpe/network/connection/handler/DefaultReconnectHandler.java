/*
 * Copyright 2023 WaterdogTEAM
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

package dev.waterdog.waterdogpe.network.connection.handler;

import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;

import java.util.List;

/**
 * This is a default reconnect handler.
 * The fallback server will always be the next one from priorities list.
 * If the old server is not defined in priorities, the fallback server will be the first server from the same list.
 */
public class DefaultReconnectHandler implements IReconnectHandler {

    @Override
    public ServerInfo getFallbackServer(ProxiedPlayer player, ServerInfo oldServer, ReconnectReason reason, String kickMessage) {
        ProxyServer proxy = ProxyServer.getInstance();
        List<String> servers = proxy.getConfiguration().getPriorities();
        if (oldServer == null) {
            return proxy.getServerInfo(servers.get(0));
        }

        int index = servers.indexOf(oldServer.getServerName());
        if (index == -1 || (index + 1) >= servers.size()) {
            return null;
        }
        return proxy.getServerInfo(servers.get(index + 1));
    }
}
