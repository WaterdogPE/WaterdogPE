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

package dev.waterdog.waterdogpe.event.defaults;

import dev.waterdog.waterdogpe.player.ProxiedPlayer;

/**
 * Called when checking player permission.
 * Can be used to modify ProxiedPlayer#hasPermission() return result without modifying permission map.
 */
public class PlayerPermissionCheckEvent extends PlayerEvent {

    private final String permission;
    private boolean hasPermission;

    public PlayerPermissionCheckEvent(ProxiedPlayer player, String permission, boolean hasPermission) {
        super(player);
        this.permission = permission;
        this.hasPermission = hasPermission;
    }

    public String getPermission() {
        return this.permission;
    }

    public void setHasPermission(boolean hasPermission) {
        this.hasPermission = hasPermission;
    }

    public boolean hasPermission() {
        return this.hasPermission;
    }
}
