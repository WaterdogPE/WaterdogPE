/**
 * Copyright 2020 WaterdogTEAM
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pe.waterdog.event.defaults;

import pe.waterdog.event.Event;
import pe.waterdog.player.ProxiedPlayer;

import java.net.InetSocketAddress;
import java.util.Collection;

public class ProxyPingEvent extends Event {

    private String motd;
    private String gameType;
    private String edition;
    private String version;

    private Collection<ProxiedPlayer> players;
    private int playerCount = -1;
    private int maximumPlayerCount;

    private final InetSocketAddress address;

    public ProxyPingEvent(String motd, String gameType, String edition, String version, Collection<ProxiedPlayer> players, int maximumPlayerCount, InetSocketAddress address){
        this.motd = motd;
        this.gameType = gameType;
        this.edition = edition;
        this.version = version;
        this.players = players;
        this.maximumPlayerCount = maximumPlayerCount;
        this.address = address;
    }

    public void setMotd(String motd) {
        this.motd = motd;
    }

    public String getMotd() {
        return this.motd;
    }

    public void setGameType(String gameType) {
        this.gameType = gameType;
    }

    public String getGameType() {
        return this.gameType;
    }

    public void setEdition(String edition) {
        this.edition = edition;
    }

    public String getEdition() {
        return this.edition;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return this.version;
    }

    public void setPlayers(Collection<ProxiedPlayer> players) {
        this.players = players;
    }

    public Collection<ProxiedPlayer> getPlayers() {
        return this.players;
    }

    public void setPlayerCount(int playerCount) {
        this.playerCount = playerCount;
    }

    public int getPlayerCount() {
        return this.playerCount < 1? this.players.size() : this.players.size();
    }

    public void setMaximumPlayerCount(int maximumPlayerCount) {
        this.maximumPlayerCount = maximumPlayerCount;
    }

    public int getMaximumPlayerCount() {
        return this.maximumPlayerCount;
    }

    public InetSocketAddress getAddress() {
        return this.address;
    }
}
