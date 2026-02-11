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

import dev.waterdog.waterdogpe.event.CancellableEvent;
import dev.waterdog.waterdogpe.event.Event;
import dev.waterdog.waterdogpe.network.protocol.user.LoginData;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import lombok.Getter;
import lombok.Setter;

import java.net.InetSocketAddress;

/**
 * Called when LoginPacket is successfully decoded and the ProxiedPlayer Class is being created.
 * loginData contains most relevant information on the client connecting
 * Can be used to create custom Player classes to override or extend the original class. (No Support)
 * Cancelling this event will lead to the player being kicked for the set cancelReason
 */
@Getter
public class PlayerAuthenticatedEvent extends Event implements CancellableEvent {

    private final LoginData loginData;
    private final InetSocketAddress address;

    @Setter
    private Class<? extends ProxiedPlayer> baseClass;
    @Setter
    private String cancelReason = "Login was cancelled";

    public PlayerAuthenticatedEvent(Class<? extends ProxiedPlayer> baseClass, LoginData loginData, InetSocketAddress address) {
        this.baseClass = baseClass;
        this.loginData = loginData;
        this.address = address;
    }

}
