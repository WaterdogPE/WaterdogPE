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

package dev.waterdog.player;

import dev.waterdog.ProxyServer;
import dev.waterdog.utils.types.Permission;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Base Player Manager, managing the registration, binding and removal of instances of ProxiedPlayer.
 */
public class PlayerManager {

    private final ProxyServer proxy;

    private final ConcurrentMap<UUID, ProxiedPlayer> players = new ConcurrentHashMap<>();

    public PlayerManager(ProxyServer proxy) {
        this.proxy = proxy;
    }

    public boolean registerPlayer(ProxiedPlayer player) {
        if (player == null) {
            return false;
        }

        ProxiedPlayer previousSession = this.players.remove(player.getUniqueId());
        if (previousSession != null && !previousSession.getUpstream().isClosed()) {
            previousSession.disconnect("disconnectionScreen.loggedinOtherLocation");
        }
        this.players.put(player.getUniqueId(), player);
        return true;
    }

    public void subscribePermissions(ProxiedPlayer player) {
        List<String> permissions = new ArrayList<>(this.proxy.getConfiguration().getDefaultPermissions());
        List<String> playerPermissions = this.proxy.getConfiguration().getPlayerPermissions().get(player.getName());
        if (playerPermissions != null) {
            permissions.addAll(playerPermissions);
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
        if (playerName == null) return null;

        for (ProxiedPlayer player : this.players.values()) {
            if (!player.getName().toLowerCase().startsWith(playerName.toLowerCase())) {
                continue;
            }

            int strLen = player.getName().length() - playerName.length();
            if (strLen == 0) return player;
        }

        return null;
    }

    public Map<UUID, ProxiedPlayer> getPlayers() {
        return Collections.unmodifiableMap(this.players);
    }
}
