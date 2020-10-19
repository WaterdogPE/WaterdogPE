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

package pe.waterdog.event.defaults;

import pe.waterdog.event.AsyncEvent;
import pe.waterdog.network.session.ServerConnection;
import pe.waterdog.player.ProxiedPlayer;

/**
 * Called when the transfer from one server to the next is completed.
 * At this point, the player is already logged in and registered to the new downstream target and the old downstream is already
 * disconnected.
 */
@AsyncEvent
public class TransferCompleteEvent extends PlayerEvent {

    private final ServerConnection oldServer;
    private final ServerConnection newServer;

    public TransferCompleteEvent(ServerConnection oldServer, ServerConnection newServer, ProxiedPlayer player) {
        super(player);
        this.oldServer = oldServer;
        this.newServer = newServer;
    }

    public ServerConnection getNewServer() {
        return this.newServer;
    }

    public ServerConnection getOldServer() {
        return this.oldServer;
    }
}

