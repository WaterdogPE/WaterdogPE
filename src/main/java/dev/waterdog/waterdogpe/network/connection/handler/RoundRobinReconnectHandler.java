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

import java.util.Collection;
import java.util.Iterator;

/**
 * This is a default reconnect handler.
 * The fallback server is chosen from the priorities list using a round-robin algorithm.
 */
public class RoundRobinReconnectHandler implements IReconnectHandler {

    private RoundRobinIterator<String> iterator;

    @Override
    public ServerInfo getFallbackServer(ProxiedPlayer player, ServerInfo oldServer, ReconnectReason reason, String kickMessage) {
        ProxyServer proxy = ProxyServer.getInstance();
        if (proxy.getConfiguration().getPriorities().size() < 2) {
            throw new IllegalStateException("RoundRobinReconnectHandler required at least two priority servers set");
        }

        if (this.iterator == null) {
            this.iterator = new RoundRobinIterator<>(proxy.getConfiguration().getPriorities());
        }

        while (iterator.hasNext()) {
            String server = iterator.next();
            if (oldServer == null || !server.equals(oldServer.getServerName())) {
                return proxy.getServerInfo(server);
            }
        }
        return null;
    }

    private static class RoundRobinIterator<E> implements Iterator<E> {
        private final Collection<E> collection;
        private Iterator<E> iterator;

        public RoundRobinIterator(Collection<E> collection) {
            this.collection = collection;
            this.iterator = this.collection.iterator();
        }

        public synchronized boolean hasNext() {
            return !this.collection.isEmpty();
        }

        public synchronized E next() {
            if (!this.iterator.hasNext()) {
                this.iterator = this.collection.iterator();
            }
            return this.iterator.next();
        }
    }
}
