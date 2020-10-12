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

import pe.waterdog.event.Event;
import pe.waterdog.network.session.LoginData;
import pe.waterdog.player.ProxiedPlayer;

import java.net.InetSocketAddress;

public class PlayerCreationEvent extends Event {

    private Class<? extends ProxiedPlayer> baseClass;
    private final LoginData loginData;
    private final InetSocketAddress address;

    public PlayerCreationEvent(Class<? extends ProxiedPlayer> baseClass, LoginData loginData, InetSocketAddress address){
        this.baseClass = baseClass;
        this.loginData = loginData;
        this.address = address;
    }

    public void setBaseClass(Class<? extends ProxiedPlayer> baseClass) {
        this.baseClass = baseClass;
    }

    public Class<? extends ProxiedPlayer> getBaseClass() {
        return this.baseClass;
    }

    public LoginData getLoginData() {
        return this.loginData;
    }

    public InetSocketAddress getAddress() {
        return this.address;
    }
}
