/*
 * Copyright 2021 WaterdogTEAM
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.waterdog.event.defaults;

import dev.waterdog.event.CancellableEvent;
import dev.waterdog.network.ServerInfo;
import dev.waterdog.player.ProxiedPlayer;

/**
 * This event will be called before an initial connection to the downstream target is made.
 * This gives us the option to completely cancel a transfer, for example to restrict access to a server.
 * Cancelling this event will simply interrupt and cancel the transfer procedure and the player will stay on the old downstream server.
 */
public class PreTransferEvent extends PlayerEvent implements CancellableEvent {

    private ServerInfo targetServer;

    public PreTransferEvent(ProxiedPlayer player, ServerInfo targetServer) {
        super(player);
        this.targetServer = targetServer;
    }

    public void setPlayer(ProxiedPlayer player) {
        this.player = player;
    }

    public ServerInfo getTargetServer() {
        return this.targetServer;
    }

    public void setTargetServer(ServerInfo targetServer) {
        this.targetServer = targetServer;
    }
}
