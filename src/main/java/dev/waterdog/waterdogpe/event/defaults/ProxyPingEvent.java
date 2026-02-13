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

import dev.waterdog.waterdogpe.event.Event;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import lombok.Getter;
import lombok.Setter;

import java.net.InetSocketAddress;
import java.util.Collection;

/**
 * This event is called when the Proxy receives a ping packet from a client.
 * It can be used to modify data, for example, to combine proxy player counts.
 */
public class ProxyPingEvent extends Event {

    @Getter
    private final InetSocketAddress address;
    @Getter
    @Setter
    private String motd;
    @Getter
    @Setter
    private String subMotd;
    @Getter
    @Setter
    private String gameType;
    @Getter
    @Setter
    private String edition;
    @Getter
    @Setter
    private String version;
    @Getter
    @Setter
    private Collection<ProxiedPlayer> players;
    @Setter
    private int playerCount = -1;
    @Setter
    @Getter
    private int maximumPlayerCount;

    public ProxyPingEvent(String motd, String subMotd, String gameType, String edition, String version, Collection<ProxiedPlayer> players, int maximumPlayerCount, InetSocketAddress address) {
        this.motd = motd;
        this.subMotd = subMotd;
        this.gameType = gameType;
        this.edition = edition;
        this.version = version;
        this.players = players;
        this.maximumPlayerCount = maximumPlayerCount;
        this.address = address;
    }

    public int getPlayerCount() {
        return this.playerCount > 0 ? this.playerCount : this.players.size();
    }

}
