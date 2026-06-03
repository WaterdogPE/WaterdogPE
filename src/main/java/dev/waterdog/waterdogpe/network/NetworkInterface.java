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

package dev.waterdog.waterdogpe.network;

import java.net.InetSocketAddress;

public interface NetworkInterface {

    /**
     * Starts the network interface, binding to the specified address and port.
     * @throws NetworkStartupException If the network interface fails to start, such as if the port is already in use or if there are insufficient permissions.
     */
    void start(InetSocketAddress address) throws NetworkStartupException;

    /**
     * Shutdowns the network interface, closing all connections and releasing resources.
     */
    void shutdown();

    /**
     * Called every tick to allow the network interface to perform any necessary updates.
     */
    default void tick() {
    }

    /**
     * Returns whether the network interface is currently running and able to accept connections.
     */
    boolean isRunning();
}
