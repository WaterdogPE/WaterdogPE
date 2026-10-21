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

package dev.waterdog.waterdogpe.utils.config.proxy;

import net.cubespace.Yamler.Config.InvalidConfigurationException;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProxyConfigTest {

    private static File newFile() throws Exception {
        return Files.createTempDirectory("waterdog-config").resolve("config.yml").toFile();
    }

    private static long comments(String text) {
        return text.lines().filter(line -> line.trim().startsWith("#")).count();
    }

    @Test
    public void rootOptionsAreAddedWithComments() throws Exception {
        File file = newFile();
        Path path = file.toPath();
        new ProxyConfig(file).init();
        String written = Files.readString(path);
        Files.writeString(path, written.replaceAll("(?m)^force_disable_vibrant_visuals:.*\\n", ""));

        new ProxyConfig(file).init();

        // Left to Yamler, a root key alone is written back with every comment dropped
        String text = Files.readString(path);
        assertTrue(text.contains("\nforce_disable_vibrant_visuals: false"));
        assertEquals(comments(written), comments(text));
    }

    @Test
    public void newFilesCarryTheCurrentVersion() throws Exception {
        File file = newFile();
        new ProxyConfig(file).init();

        assertTrue(Files.readString(file.toPath()).contains("\nconfig_version: " + ConfigMigrations.CURRENT + "\n"));
    }

    /**
     * Loads a file of an older layout, then checks that running the migration again on the result
     * leaves it as it is.
     */
    private static ProxyConfig migrate(File file, String text) throws Exception {
        Path path = file.toPath();
        Files.writeString(path, text);
        ProxyConfig config = new ProxyConfig(file);
        config.init();

        String migrated = Files.readString(path);
        String version = "\nconfig_version: " + ConfigMigrations.CURRENT + "\n";
        assertTrue(migrated.contains(version));
        Files.writeString(path, migrated.replace(version, "\nconfig_version: 0\n"));
        new ProxyConfig(file).init();
        assertEquals(migrated, Files.readString(path), "a second run changed the file");
        return config;
    }

    @Test
    public void upstreamNetherNetLayoutIsMigrated() throws Exception {
        // As WaterdogPE wrote it in October 2026, before config_version
        File file = newFile();
        ProxyConfig config = migrate(file, """
                nethernet:
                  enabled: true
                  signaling_mode: hybrid
                  nxs:
                    token: secret
                    endpoint: https://nxs.example.com
                    advertise_addresses: ['play.example.com:19133', '8.8.8.8:19133']
                    advanced:
                      assisted_joins: true
                      connection_checks: true
                      stun_discovery: false
                      location: {}
                      deregister_on_shutdown: false
                  trusted_proxies: ['10.0.0.0/8']
                  proxy_protocol: true
                  advertise_addresses: ['1.1.1.1', ' play.example.com:19133 ']
                  udp_port: 0
                  https:
                    certificate: certs/fullchain.pem
                    private_key: certs/privkey.pem
                    password: ''
                  identity_file: keys/fleet.pem
                """);

        assertEquals(ProxyConfig.Transport.BOTH, config.getTransport());
        NetherNetSettings settings = config.getNetherNetSettings();
        SignalingSettings signaling = settings.getSignaling();
        assertEquals(0, settings.getWebrtcPort());
        assertEquals("keys/fleet.pem", settings.getIdentityFile());
        assertEquals(SignalingSettings.SignalingMode.HYBRID, signaling.getMode());
        // One list, each address once, built-in signaling's first
        assertEquals(List.of("1.1.1.1", "play.example.com:19133", "8.8.8.8:19133"), signaling.getAdvertiseAddresses());
        assertFalse(signaling.isStunDiscovery());
        assertEquals(List.of("10.0.0.0/8"), signaling.getTrustedProxies());
        assertTrue(signaling.isProxyProtocol());
        assertEquals("certs/fullchain.pem", signaling.getBuiltin().getHttps().getCertificate());
        assertEquals("secret", signaling.getNxs().getToken());
        assertEquals("https://nxs.example.com", signaling.getNxs().getEndpoint());
        assertTrue(signaling.getNxs().getAdvanced().isAssistedJoins());

        String text = Files.readString(file.toPath());
        for (String moved : List.of("  enabled:", "udp_port:", "signaling_mode:", "\n  nxs:", "\n  https:", "\n  advertise_addresses:", "      stun_discovery:")) {
            assertFalse(text.contains(moved), moved + " stays in the file");
        }
    }

    @Test
    public void unreleasedNetherNetLayoutIsMigrated() throws Exception {
        // Built-in signaling and NXS already share their options, as on builds after October 18
        ProxyConfig config = migrate(newFile(), """
                nethernet:
                  enabled: false
                  signaling_mode: nxs
                  nxs:
                    token: secret
                    endpoint: https://nxs.example.com
                    advanced:
                      assisted_joins: false
                      connection_checks: false
                      location: {country: nl}
                      deregister_on_shutdown: true
                  trusted_proxies: []
                  proxy_protocol: false
                  advertise_addresses: ['play.example.com']
                  stun_discovery: false
                  udp_port: 19140
                  https:
                    certificate: ''
                    private_key: ''
                    password: ''
                  identity_file: keys/identity.pem
                """);

        assertEquals(ProxyConfig.Transport.RAKNET, config.getTransport());
        NetherNetSettings settings = config.getNetherNetSettings();
        SignalingSettings signaling = settings.getSignaling();
        assertEquals(19140, settings.getWebrtcPort());
        assertEquals(SignalingSettings.SignalingMode.NXS, signaling.getMode());
        assertEquals(List.of("play.example.com"), signaling.getAdvertiseAddresses());
        assertFalse(signaling.isStunDiscovery());
        assertFalse(signaling.getNxs().getAdvanced().isConnectionChecks());
        assertTrue(signaling.getNxs().getAdvanced().isDeregisterOnShutdown());
        assertEquals("nl", signaling.getNxs().getAdvanced().getLocation().get("country"));
    }

    @Test
    public void netherNetAloneTakesTheListenerPort() throws Exception {
        File file = newFile();
        Files.writeString(file.toPath(), "listener:\n  transport: nethernet\n");
        ProxyConfig config = new ProxyConfig(file);
        config.init();

        // RakNet does not hold it, so media shares the port players already know
        NetherNetSettings settings = config.getNetherNetSettings();
        assertEquals(19132, settings.resolveWebrtcPort(19132, config.getTransport().raknet(), Set.of(19133)));
        String text = Files.readString(file.toPath());
        assertTrue(text.contains("\n  transport: nethernet\n"));
        assertTrue(text.contains("\n  webrtc_port: 0\n"), "0 is replaced in the file");
    }

    @Test
    public void unknownTransportsFailTheLoad() throws Exception {
        File file = newFile();
        Files.writeString(file.toPath(), "listener:\n  transport: nethernt\n");

        Throwable failure = assertThrows(InvalidConfigurationException.class, () -> new ProxyConfig(file).init());
        while (failure.getCause() != null) {
            failure = failure.getCause();
        }
        assertEquals("\"nethernt\" is not a valid transport, expected one of raknet, nethernet, both", failure.getMessage());
    }

    @Test
    public void currentFilesAreLeftAsTheyAre() throws Exception {
        File file = newFile();
        new ProxyConfig(file).init();

        migrate(file, Files.readString(file.toPath()));
    }

    @Test
    public void negativeVersionsAreRefused() throws Exception {
        File file = newFile();
        Files.writeString(file.toPath(), "config_version: -1\n");

        InvalidConfigurationException e = assertThrows(InvalidConfigurationException.class,
                () -> new ProxyConfig(file).init());
        assertTrue(e.getMessage().contains("invalid config_version -1"), e.getMessage());
    }

    @Test
    public void newerFilesAreRefusedUntouched() throws Exception {
        File file = newFile();
        Path path = file.toPath();
        new ProxyConfig(file).init();
        int newer = ConfigMigrations.CURRENT + 1;
        // Drops an option too, which an older proxy would otherwise write back
        Files.writeString(path, Files.readString(path)
                .replace("\nconfig_version: " + ConfigMigrations.CURRENT + "\n", "\nconfig_version: " + newer + "\n")
                .replaceAll("(?m)^force_disable_vibrant_visuals:.*\\n", ""));
        byte[] before = Files.readAllBytes(path);

        InvalidConfigurationException e = assertThrows(InvalidConfigurationException.class,
                () -> new ProxyConfig(file).init());
        assertTrue(e.getMessage().contains("newer WaterdogPE"), e.getMessage());
        assertArrayEquals(before, Files.readAllBytes(path));
    }
}
