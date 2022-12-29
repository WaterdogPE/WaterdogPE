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

import java.net.InetSocketAddress;
import java.util.Collection;

/**
 * Called when the Proxy receives an MCPEQuery. Returns, in addition to the ProxyPingEvent, a map and whether a whitelist is present.
 * Can be modified to change the returned values.
 */
public class ProxyQueryEvent extends ProxyPingEvent {

    private String map;
    private boolean hasWhitelist = false;

    public ProxyQueryEvent(String motd, String gameType, String edition, String version, Collection<ProxiedPlayer> players, int maximumPlayerCount, String map, InetSocketAddress address) {
        super(motd, "", gameType, edition, version, players, maximumPlayerCount, address);
        this.map = map;
    }

    public String getMap() {
        return this.map;
    }

    public void setMap(String map) {
        this.map = map;
    }

    public void setHasWhitelist(boolean hasWhitelist) {
        this.hasWhitelist = hasWhitelist;
    }

    public boolean hasWhitelist() {
        return this.hasWhitelist;
    }
}
