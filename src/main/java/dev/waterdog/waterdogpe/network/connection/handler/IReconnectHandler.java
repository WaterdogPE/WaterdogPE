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
 * Called whenever a client is being kicked from a downstream server
 * Can be used to easily set up a fallback to transfer the player to another server
 */
public interface IReconnectHandler {

    /**
     * @param player    the player who got kicked by downstream
     * @param oldServer the ServerInfo of the downstream server who kicked the player
     * @return a ServerInfo if there was a valid server found for fallback, or null if no server was found. null will lead to the player getting kicked.
     */
    default ServerInfo getFallbackServer(ProxiedPlayer player, ServerInfo oldServer, ReconnectReason reason, String kickMessage) {
        return this.getFallbackServer(player, oldServer, kickMessage); // backward compatibility
    }

    /**
     * Called when the connection to a new downstream server fails before StartGamePacket was received.
     * At this point the player is still fully connected to the previous downstream server, so unlike
     * {@link #getFallbackServer} this failure is recoverable: returning null (or the server the player
     * is currently on) keeps the player where they are, returning any other server transfers them to it.
     * Defaults to a regular reconnect through {@link #getFallbackServer}.
     *
     * @param player       the player whose transfer failed
     * @param targetServer the ServerInfo of the server that could not be reached
     * @return the server to reconnect the player to, or null to stay on the current server
     */
    default ServerInfo getTransferFailureServer(ProxiedPlayer player, ServerInfo targetServer, ReconnectReason reason, String kickMessage) {
        return this.getFallbackServer(player, targetServer, reason, kickMessage);
    }

    @Deprecated
    default ServerInfo getFallbackServer(ProxiedPlayer player, ServerInfo oldServer, String kickMessage) {
        throw new UnsupportedOperationException("Use getFallbackServer(ProxiedPlayer player, ServerInfo oldServer, ReconnectReason reason, String kickMessage) instead");
    }
}
