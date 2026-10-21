/*
 * Copyright 2026 WaterdogTEAM
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

package dev.waterdog.waterdogpe.network.nethernet;

import dev.waterdog.waterdogpe.event.defaults.ProxyPingEvent;
import org.cloudburstmc.netty.signaling.ProviderClient;
import org.cloudburstmc.netty.signaling.ServerStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class NetherNetProviderStatusTest {

    private static final ProviderClient.Software SOFTWARE = new ProviderClient.Software("WaterdogPE", "test");

    /** A ping as the proxy builds it, before plugins: listener.max_players is 1 and no one is on. */
    private static ProxyPingEvent ping() {
        return new ProxyPingEvent("Config MOTD", "Config sub", "Survival", "MCPE", "1.21.0", List.of(), 1, null);
    }

    @Test
    public void listsWhatPluginsSetOnThePing() {
        ProxyPingEvent ping = ping();
        ping.setMotd("§bNetwork");
        ping.setSubMotd("Play Now!");
        ping.setPlayerCount(800);
        ping.setMaximumPlayerCount(810);
        ping.setGameType("Creative");

        assertEquals(new ServerStatus("§bNetwork", "Play Now!", 810, 1), NetherNetProvider.status(ping));
        ProviderClient.Health health = NetherNetProvider.health(ping, false, SOFTWARE);
        assertEquals(810, health.capacity());
        assertEquals(800, health.connectedPlayers().count());
    }

    @Test
    public void listsTheListenerWithoutPlugins() {
        ProxyPingEvent ping = ping();

        assertEquals(new ServerStatus("Config MOTD", "Config sub", 1, 0), NetherNetProvider.status(ping));
        assertEquals(0, NetherNetProvider.health(ping, false, SOFTWARE).connectedPlayers().count());
    }

    @Test
    public void onlyAnExplicitLimitTurnsPlayersAway() {
        ProxyPingEvent ping = ping();
        ping.setPlayerCount(5);

        // Past listener.max_players, which is often set for display
        assertTrue(NetherNetProvider.health(ping, false, SOFTWARE).acceptingPlayers());
        assertFalse(NetherNetProvider.health(ping, true, SOFTWARE).acceptingPlayers());
    }

    @Test
    public void keepsCountsWithinWhatTheProviderTakes() {
        ProxyPingEvent ping = ping();
        ping.setPlayerCount(2_000_000);
        ping.setMaximumPlayerCount(-1);

        assertEquals(0, NetherNetProvider.status(ping).maxPlayerCount());
        ProviderClient.Health health = NetherNetProvider.health(ping, false, SOFTWARE);
        assertEquals(0, health.capacity());
        assertEquals(1_000_000, health.connectedPlayers().count());
    }
}
