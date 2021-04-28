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

import dev.waterdog.waterdogpe.event.AsyncEvent;
import dev.waterdog.waterdogpe.event.Event;

import java.net.InetSocketAddress;

/**
 * Called when pending connection wasn't accepted due failed XBOX authentication or
 * when exception happened during processing handshake data.
 */
@AsyncEvent
public class PreLoginFailedEvent extends Event {

    private final InetSocketAddress address;
    private final boolean xboxAuthenticated;
    private final Throwable throwable;

    private String disconnectMessage;

    public PreLoginFailedEvent(InetSocketAddress address, boolean xboxAuthenticated, Throwable throwable, String disconnectMessage) {
        this.address = address;
        this.xboxAuthenticated = xboxAuthenticated;
        this.throwable = throwable;
        this.disconnectMessage = disconnectMessage;
    }

    public InetSocketAddress getAddress() {
        return this.address;
    }

    public boolean isXboxAuthenticated() {
        return this.xboxAuthenticated;
    }

    public Throwable getThrowable() {
        return this.throwable;
    }

    public void setDisconnectMessage(String disconnectMessage) {
        this.disconnectMessage = disconnectMessage;
    }

    public String getDisconnectMessage() {
        return this.disconnectMessage;
    }
}
