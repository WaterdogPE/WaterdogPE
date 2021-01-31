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

package dev.waterdog.event.defaults;

import dev.waterdog.event.AsyncEvent;
import dev.waterdog.network.session.ServerConnection;
import dev.waterdog.player.ProxiedPlayer;

/**
 * Called when PlayStatusPacket with PLAYER_SPAWN from downstream server is received.
 * At this point player is really connected to downstream server. Some downstream servers have different handlers per login stage.
 * To signalize last stage of login, we can use this event. For example applies on PMMP4 downstream servers.
 */
@AsyncEvent
public class PostTransferCompleteEvent extends PlayerEvent {

    private final ServerConnection server;

    public PostTransferCompleteEvent(ServerConnection server, ProxiedPlayer player) {
        super(player);
        this.server = server;
    }

    public ServerConnection getServer() {
        return this.server;
    }
}

