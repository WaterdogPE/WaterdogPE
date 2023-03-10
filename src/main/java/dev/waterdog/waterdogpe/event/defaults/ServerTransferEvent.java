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

import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;

/**
 * Signalizing that player is being transferred to a new server.
 * This even is not cancellable. Use PreTransferEvent to cancel transfer.
 */
public class ServerTransferEvent extends PlayerEvent {

    private final ServerInfo sourceServer;
    private final ServerInfo targetServer;
    private boolean transferScreenAllowed = true;

    public ServerTransferEvent(ProxiedPlayer player, ServerInfo sourceServer, ServerInfo targetServer) {
        super(player);
        this.sourceServer = sourceServer;
        this.targetServer = targetServer;
    }

    public ServerInfo getSourceServer() {
        return this.sourceServer;
    }

    public ServerInfo getTargetServer() {
        return this.targetServer;
    }

    public void setTransferScreenAllowed(boolean transferScreenAllowed) {
        this.transferScreenAllowed = transferScreenAllowed;
    }

    public boolean isTransferScreenAllowed() {
        return this.transferScreenAllowed;
    }
}

