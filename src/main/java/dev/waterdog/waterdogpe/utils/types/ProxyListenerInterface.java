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

package dev.waterdog.waterdogpe.utils.types;

import java.net.SocketAddress;

/**
 * This interface is used as alternative to events in situation when calling an event can expensive and
 * the benefit of using event is not greater.
 */
public interface ProxyListenerInterface {

    /**
     * Called once new session for connection is being created.
     * @param address of the sender.
     * @return if new session can be created.
     */
    default boolean onConnectionCreation(SocketAddress address) {
        return true;
    }

    /**
     * Called once new player sends LoginPacket.
     * @param address of the sender.
     * @return true if login is allowed.
     */
    default boolean onLoginAttempt(SocketAddress address) {
        return true;
    }

    /**
     * Called when incompatible protocol version is found in LoginPacket.
     * @param protocolVersion game version of connection.
     * @param address of the sender.
     */
    default void onIncorrectVersionLogin(int protocolVersion, SocketAddress address) {
    }

    /**
     * Called when pending connection wasn't accepted due failed XBOX authentication or
     * when exception happened during processing handshake data.
     * @param address of the sender.
     * @param xboxAuth if player is XBOX authenticated.
     * @param throwable thrown exception or null.
     * @param reason if login is allowed.
     * @return disconnect message sent to client.
     */
    default String onLoginFailed(SocketAddress address, boolean xboxAuth, Throwable throwable, String reason) {
        return reason;
    }
}
