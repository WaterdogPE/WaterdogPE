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
import dev.waterdog.waterdogpe.network.connection.client.ClientConnection;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;

/**
 * Called when a player successfully logged in to the initial server. This event is not called when the transfer from
 * one server to another completed successfully, use {@link TransferCompleteEvent} for that purpose instead. At this
 * point the player associated with the event is already logged in and registered to the initial downstream.
 */
@AsyncEvent
public class InitialServerConnectedEvent extends PlayerEvent {

    private final ClientConnection connection;

    public InitialServerConnectedEvent(ProxiedPlayer player, ClientConnection connection) {
        super(player);
        this.connection = connection;
    }

    public ClientConnection getConnection() {
        return this.connection;
    }

    public ServerInfo getServerInfo() {
        return this.connection.getServerInfo();
    }
}
