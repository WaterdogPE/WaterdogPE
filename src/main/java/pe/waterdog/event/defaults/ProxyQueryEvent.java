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

public class ProxyQueryEvent extends Event {

    private String motd;
    private String gameType;
    private String edition;
    private String version;

    private int playerCount;
    private int maximumPlayerCount;

    public ProxyQueryEvent(String motd, String gameType, String edition, String version, int playerCount, int maximumPlayerCount){
        this.motd = motd;
        this.gameType = gameType;
        this.edition = edition;
        this.version = version;
        this.playerCount = playerCount;
        this.maximumPlayerCount = maximumPlayerCount;
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

    public void setPlayerCount(int playerCount) {
        this.playerCount = playerCount;
    }

    public int getPlayerCount() {
        return this.playerCount;
    }

    public void setMaximumPlayerCount(int maximumPlayerCount) {
        this.maximumPlayerCount = maximumPlayerCount;
    }

    public int getMaximumPlayerCount() {
        return this.maximumPlayerCount;
    }
}
