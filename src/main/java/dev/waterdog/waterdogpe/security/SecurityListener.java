/*
 * Copyright 2023 WaterdogTEAM
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

package dev.waterdog.waterdogpe.security;

import dev.waterdog.waterdogpe.network.protocol.user.HandshakeEntry;

import java.net.InetAddress;
import java.net.SocketAddress;

public interface SecurityListener {

    /**
     * Called once new session for connection is being created.
     * @param address of the sender.
     * @return if new session can be created.
     */
    default boolean onConnectionCreated(SocketAddress address) {
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
     * Called when pending connection wasn't accepted due failed XBOX authentication or
     * when exception happened during processing handshake data.
     * @param address of the sender.
     * @param handshakeEntry the Handshake information of failed connection.
     * @param throwable thrown exception or null.
     * @param reason if login is allowed.
     * @return disconnect message sent to client.
     */
    default String onLoginFailed(SocketAddress address, HandshakeEntry handshakeEntry, Throwable throwable, String reason) {
        return reason;
    }

    /**
     * Called when specific address reaches a passed throttle limit
     * @param address address of the connection
     * @param throttle throttle used to determine limit
     */
    default void onThrottleReached(InetAddress address, ConnectionThrottle throttle) {
    }
}
