/*
 * Copyright 2026 WaterdogTEAM
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
import dev.waterdog.waterdogpe.network.NetworkInterface;
import lombok.Getter;

/**
 * Called whenever a network interface is registered via {@link dev.waterdog.waterdogpe.ProxyServer#registerInterface}.
 * This event can be cancelled to prevent the interface from being registered.
 */
@Getter
public class NetworkRegisterEvent extends Event implements CancellableEvent {

    private final NetworkInterface networkInterface;

    public NetworkRegisterEvent(NetworkInterface networkInterface) {
        this.networkInterface = networkInterface;
    }
}
