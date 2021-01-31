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
import dev.waterdog.event.CancellableEvent;
import dev.waterdog.player.ProxiedPlayer;

/**
 * Called right before the initial connect is made.
 * Cancelling it will close the connection with the cancelReason as the disconnectMessage.
 */
@AsyncEvent
public class PlayerLoginEvent extends PlayerEvent implements CancellableEvent {

    private String cancelReason = "Login cancelled";

    public PlayerLoginEvent(ProxiedPlayer player) {
        super(player);
    }

    public String getCancelReason() {
        return this.cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }
}
