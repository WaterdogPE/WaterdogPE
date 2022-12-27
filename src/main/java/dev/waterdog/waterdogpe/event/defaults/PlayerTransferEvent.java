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

package dev.waterdog.waterdogpe.event.defaults;

import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo;
import dev.waterdog.waterdogpe.network.session.DownstreamClient;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;

/**
 * Signalizing that player is being transferred to a new server.
 * This even is not cancellable. Use PreTransferEvent to cancel transfer.
 */
public class PlayerTransferEvent extends PlayerEvent {

    private final ServerInfo oldServer;
    private final DownstreamClient serverConnection;

    public PlayerTransferEvent(ProxiedPlayer player, ServerInfo oldServer, DownstreamClient serverConnection) {
        super(player);
        this.oldServer = oldServer;
        this.serverConnection = serverConnection;
    }

    public ServerInfo getOldServer() {
        return this.oldServer;
    }

    public DownstreamClient getServerConnection() {
        return this.serverConnection;
    }
}

