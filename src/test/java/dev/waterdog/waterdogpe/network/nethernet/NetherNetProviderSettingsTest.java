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

import dev.waterdog.waterdogpe.utils.config.proxy.ProxyConfig;
import org.cloudburstmc.netty.signaling.ProviderClient;
import org.cloudburstmc.netty.signaling.provider.ProviderRuntimeConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class NetherNetProviderSettingsTest {

    private static ProviderRuntimeConfiguration.Settings settings(String nxs) throws Exception {
        File file = Files.createTempDirectory("waterdog-config").resolve("config.yml").toFile();
        Files.writeString(file.toPath(), "nethernet:\n  nxs:\n" + nxs.indent(4));
        ProxyConfig config = new ProxyConfig(file);
        config.init();
        return NetherNetProvider.providerSettings(config.getNetherNetSettings().getNxs());
    }

    @Test
    public void defaultsCheckConnectivityWithoutAssistance() throws Exception {
        var settings = settings("token: secret\n");
        assertEquals("secret", settings.token());
        assertFalse(settings.assistedJoins());
        assertTrue(settings.connectionChecks());
        assertTrue(settings.stunDiscovery());
        assertFalse(settings.deregisterOnShutdown());
        assertNull(settings.location());
        assertTrue(settings.gameOutcomes(), "The proxy reports game outcomes itself");
        assertNull(settings.label(), "The provider names the instance after the host");
    }

    @Test
    public void passesTheAdvancedOptions() throws Exception {
        var settings = settings("""
                advanced:
                  assisted_joins: true
                  connection_checks: false
                  stun_discovery: false
                  deregister_on_shutdown: true
                  location: {country: nl, latitude: '52.37', longitude: '4.89'}
                """);
        assertTrue(settings.assistedJoins());
        assertFalse(settings.connectionChecks());
        assertFalse(settings.stunDiscovery());
        assertTrue(settings.deregisterOnShutdown());
        assertEquals(new ProviderClient.Location("NL", null, 52.37, 4.89), settings.location());
    }

    @Test
    public void refusesAnInvalidLocation() {
        for (String location : List.of("{city: London}", "{country: GBR}", "{latitude: '91', longitude: '0'}")) {
            assertThrows(IOException.class, () -> settings("advanced:\n  location: " + location + "\n"), location);
        }
    }
}
