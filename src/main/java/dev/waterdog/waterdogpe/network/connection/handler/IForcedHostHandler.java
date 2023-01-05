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

package dev.waterdog.waterdogpe.network.connection.handler;

import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;

public interface IForcedHostHandler {

    /**
     * This handler can be overwritten to implement custom forced-host handler logic.
     * @param domain The domain used in Minecraft's "Address" tab when connecting
     * @param player The player object of the player that tried to join
     * @return A ServerInfo object of the server that player should be moved to, or null if it should use priorities instead
     */
    ServerInfo resolveForcedHost(String domain, ProxiedPlayer player);
}
