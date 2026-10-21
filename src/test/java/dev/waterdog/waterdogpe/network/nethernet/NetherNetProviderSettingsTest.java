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
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class NetherNetProviderSettingsTest {

    private static ProviderRuntimeConfiguration.Settings settings(String signaling) throws Exception {
        return settings(Files.createTempDirectory("waterdog-config"), signaling);
    }

    private static ProviderRuntimeConfiguration.Settings settings(Path dir, String signaling) throws Exception {
        File file = dir.resolve("config.yml").toFile();
        Files.writeString(file.toPath(), "nethernet:\n  signaling:\n" + signaling.indent(4));
        ProxyConfig config = new ProxyConfig(file);
        config.init();
        return NetherNetProvider.providerSettings(config.getNetherNetSettings().getSignaling(), dir);
    }

    @Test
    public void readsATokenFileRelativeToTheProxy() throws Exception {
        Path dir = Files.createTempDirectory("waterdog-config");
        Files.writeString(dir.resolve("nxs-token.txt"), "secret\n");

        assertEquals("secret", settings(dir, "nxs:\n  token: file:nxs-token.txt\n").token());
    }

    @Test
    public void movesTheRegistrationIntoTheNetherNetFolder() throws Exception {
        Path dir = Files.createTempDirectory("waterdog-data");
        Files.createDirectories(dir.resolve("provider-state"));
        Files.writeString(dir.resolve("provider-state/state.json"), "old");

        NetherNetProvider.moveLegacyState(dir);

        assertFalse(Files.exists(dir.resolve("provider-state")));
        assertEquals("old", Files.readString(dir.resolve("nethernet/provider-state/state.json")));
    }

    @Test
    public void aRegistrationThatCannotMoveStopsTheStart() throws Exception {
        Path dir = Files.createTempDirectory("waterdog-data");
        Files.createDirectories(dir.resolve("provider-state"));
        // A file where the folder goes, as a stand-in for a move the file system refuses
        Files.writeString(dir.resolve("nethernet"), "");

        IOException e = assertThrows(IOException.class, () -> NetherNetProvider.moveLegacyState(dir));
        assertTrue(e.getMessage().contains("move it there by hand"), e.getMessage());
        assertTrue(Files.exists(dir.resolve("provider-state")));
    }

    @Test
    public void keepsARegistrationAlreadyInTheNetherNetFolder() throws Exception {
        Path dir = Files.createTempDirectory("waterdog-data");
        Files.createDirectories(dir.resolve("provider-state"));
        Files.createDirectories(dir.resolve("nethernet/provider-state"));
        Files.writeString(dir.resolve("nethernet/provider-state/state.json"), "current");

        NetherNetProvider.moveLegacyState(dir);

        assertTrue(Files.exists(dir.resolve("provider-state")), "the old folder is left for the operator");
        assertEquals("current", Files.readString(dir.resolve("nethernet/provider-state/state.json")));
    }

    @Test
    public void defaultsCheckConnectivityWithoutAssistance() throws Exception {
        var settings = settings("nxs:\n  token: secret\n");
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
                stun_discovery: false
                nxs:
                  advanced:
                    assisted_joins: true
                    connection_checks: false
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
    public void takesTheAddressesBuiltInSignalingAdvertises() throws Exception {
        var settings = settings("advertise_addresses: ['1.1.1.1:19133', 'play.example.com']\n");
        assertEquals(List.of("1.1.1.1:19133", "play.example.com"), settings.advertiseAddresses());
    }

    @Test
    public void refusesAnInvalidLocation() {
        for (String location : List.of("{city: London}", "{country: GBR}", "{latitude: '91', longitude: '0'}")) {
            assertThrows(IOException.class, () -> settings("nxs:\n  advanced:\n    location: " + location + "\n"), location);
        }
    }
}
