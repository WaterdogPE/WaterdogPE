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

/**
 * Interface that can be implemented and assigned to the server.
 * The JoinHandler is called whenever a player establishes an initial connection.
 * Its job is to determine what the initial server of the client should be.
 */
public interface IJoinHandler {

    /**
     * determines the initial server
     *
     * @param player the player who is connecting to the server
     * @return ServerInfo if a server is found, or null if no server was found. null will lead to the player getting kicked.
     */
    ServerInfo determineServer(ProxiedPlayer player);
}
