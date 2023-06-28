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

import java.net.InetSocketAddress;
import java.util.Collection;

/**
 * This event is called when the Proxy receives a ping packet from a client.
 * It can be used to modify data, for example to combine proxy player counts.
 */
public class ProxyPingEvent extends Event {

    private final InetSocketAddress address;
    private String motd;
    private String subMotd;
    private String gameType;
    private String edition;
    private String version;
    private Collection<ProxiedPlayer> players;
    private int playerCount = -1;
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

    public String getMotd() {
        return this.motd;
    }

    public void setMotd(String motd) {
        this.motd = motd;
    }

    public String getSubMotd() {
        return this.subMotd;
    }

    public void setSubMotd(String subMotd) {
        this.subMotd = subMotd;
    }

    public String getGameType() {
        return this.gameType;
    }

    public void setGameType(String gameType) {
        this.gameType = gameType;
    }

    public String getEdition() {
        return this.edition;
    }

    public void setEdition(String edition) {
        this.edition = edition;
    }

    public String getVersion() {
        return this.version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Collection<ProxiedPlayer> getPlayers() {
        return this.players;
    }

    public void setPlayers(Collection<ProxiedPlayer> players) {
        this.players = players;
    }

    public int getPlayerCount() {
        return this.playerCount > 0 ? this.playerCount : this.players.size();
    }

    public void setPlayerCount(int playerCount) {
        this.playerCount = playerCount;
    }

    public int getMaximumPlayerCount() {
        return this.maximumPlayerCount;
    }

    public void setMaximumPlayerCount(int maximumPlayerCount) {
        this.maximumPlayerCount = maximumPlayerCount;
    }

    public InetSocketAddress getAddress() {
        return this.address;
    }
}
