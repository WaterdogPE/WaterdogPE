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

import dev.waterdog.waterdogpe.event.AsyncEvent;
import dev.waterdog.waterdogpe.event.CancellableEvent;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import lombok.Getter;

/**
 * Called right before a connection to the target downstream server is opened, after the transfer has
 * been accepted (see {@link ServerTransferRequestEvent}) but before {@code createConnection} is invoked.
 * <p>
 * This event is asynchronous and completable: a handler may register a {@link java.util.concurrent.CompletableFuture}
 * via {@link #addCompletableFuture(java.util.concurrent.CompletableFuture)} to hold out the connection until that
 * future settles. This is useful for work that must finish on the current server first, for example saving the
 * player's inventory or other state before they leave. The proxy dials the target only once every registered
 * future has completed.
 */
@Getter
@AsyncEvent
public class ServerPreConnectEvent extends PlayerEvent implements CancellableEvent {

    /**
     * The server the player is currently connected to, or {@code null} on the initial connection.
     */
    private final ServerInfo sourceServer;
    private final ServerInfo targetServer;

    public ServerPreConnectEvent(ProxiedPlayer player, ServerInfo sourceServer, ServerInfo targetServer) {
        super(player);
        this.sourceServer = sourceServer;
        this.targetServer = targetServer;
    }

}