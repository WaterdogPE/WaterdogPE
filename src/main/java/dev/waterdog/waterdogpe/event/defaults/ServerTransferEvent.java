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
import lombok.Getter;
import lombok.Setter;

/**
 * Signalizing that player is being transferred to a new server.
 * This even is not cancellable. Use PreTransferEvent to cancel transfer.
 */
@Getter
public class ServerTransferEvent extends PlayerEvent {

    private final ServerInfo sourceServer;
    private final ServerInfo targetServer;
    @Setter
    private boolean transferScreenAllowed = true;

    public ServerTransferEvent(ProxiedPlayer player, ServerInfo sourceServer, ServerInfo targetServer) {
        super(player);
        this.sourceServer = sourceServer;
        this.targetServer = targetServer;
    }

}

