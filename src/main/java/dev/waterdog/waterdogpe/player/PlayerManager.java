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

package dev.waterdog.waterdogpe.player;

import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.utils.types.Permission;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Base Player Manager, managing the registration, binding and removal of instances of ProxiedPlayer.
 */
public class PlayerManager {

    private final ProxyServer proxy;

    private final Map<UUID, ProxiedPlayer> players = new ConcurrentHashMap<>();

    public PlayerManager(ProxyServer proxy) {
        this.proxy = proxy;
    }

    public boolean registerPlayer(ProxiedPlayer player) {
        if (player == null) {
            return false;
        }

        ProxiedPlayer previousSession = this.players.remove(player.getUniqueId());
        if (previousSession != null && previousSession.getConnection().isConnected()) {
            previousSession.disconnect("disconnectionScreen.loggedinOtherLocation");
        }
        this.players.put(player.getUniqueId(), player);
        return true;
    }

    public void subscribePermissions(ProxiedPlayer player) {
        this.proxy.getConfiguration().getDefaultPermissions().forEach(perm -> player.addPermission(new Permission(perm, true)));
        List<String> permissions = this.proxy.getConfiguration().getPlayerPermissions().get(player.getName());
        if (permissions == null) {
            return;
        }

        for (String perm : permissions) {
            player.addPermission(new Permission(perm, true));
        }
    }

    public void removePlayer(ProxiedPlayer player) {
        if (player != null) {
            this.players.remove(player.getUniqueId());
        }
    }

    public ProxiedPlayer getPlayer(UUID uuid) {
        return uuid == null ? null : this.players.get(uuid);
    }

    public ProxiedPlayer getPlayer(String playerName) {
        if (playerName == null) {
            return null;
        }

        for (ProxiedPlayer player : this.players.values()) {
            if (!player.getName().toLowerCase().startsWith(playerName.toLowerCase())) {
                continue;
            }
            if (player.getName().length() - playerName.length() == 0) {
                return player;
            }
        }
        return null;
    }
    
    public int getPlayerCount() {
        return this.players.size();
    }

    public Map<UUID, ProxiedPlayer> getPlayers() {
        return Collections.unmodifiableMap(this.players);
    }
}
