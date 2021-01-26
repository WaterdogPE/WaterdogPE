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

import pe.waterdog.event.CancellableEvent;
import pe.waterdog.event.Event;
import pe.waterdog.network.session.LoginData;
import pe.waterdog.player.ProxiedPlayer;

import java.net.InetSocketAddress;

/**
 * Called when LoginPacket is successfully decoded and the ProxiedPlayer Class is being created.
 * loginData contains most relevant information on the client connecting
 * Can be used to create custom Player classes to override or extend the original class. (No Support)
 * Cancelling this event will lead to the player being kicked for the set cancelReason
 */
public class PlayerPreLoginEvent extends Event implements CancellableEvent {

    private final LoginData loginData;
    private final InetSocketAddress address;

    private Class<? extends ProxiedPlayer> baseClass;
    private String cancelReason = "Login was cancelled";

    public PlayerPreLoginEvent(Class<? extends ProxiedPlayer> baseClass, LoginData loginData, InetSocketAddress address) {
        this.baseClass = baseClass;
        this.loginData = loginData;
        this.address = address;
    }

    public LoginData getLoginData() {
        return this.loginData;
    }

    public InetSocketAddress getAddress() {
        return this.address;
    }

    public Class<? extends ProxiedPlayer> getBaseClass() {
        return this.baseClass;
    }

    public void setBaseClass(Class<? extends ProxiedPlayer> baseClass) {
        this.baseClass = baseClass;
    }

    public String getCancelReason() {
        return this.cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }
}
