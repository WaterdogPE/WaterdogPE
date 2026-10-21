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

package dev.waterdog.waterdogpe.utils.config;

import dev.waterdog.waterdogpe.utils.config.proxy.HttpsSettings;
import dev.waterdog.waterdogpe.utils.config.proxy.NetherNetSettings;
import dev.waterdog.waterdogpe.utils.config.proxy.ProxyConfig;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SettingsSectionTest {

    private static String write() throws Exception {
        File file = Files.createTempDirectory("waterdog-config").resolve("config.yml").toFile();
        ProxyConfig config = new ProxyConfig(file);
        config.save();
        return Files.readString(file.toPath());
    }

    @Test
    public void sectionsAreDocumented() throws Exception {
        String text = write();
        // Yamler only reads comments off the root, so a section without help writes none of these
        assertTrue(text.contains("# Accept NetherNet connections"), "nethernet section is undocumented");
        assertTrue(text.contains("# Serves signaling over HTTPS as well as HTTP, on the same port"));
        assertTrue(text.contains("# PEM private key for the certificate"), "https subsection is undocumented");
        assertTrue(text.contains("# Maximum MTU size of user <-> proxy connection that is allowed"));
    }

    @Test
    public void sectionsKeepDeclarationOrder() throws Exception {
        String text = write();
        assertTrue(text.indexOf("  enabled: true") < text.indexOf("  signaling_mode:"));
        assertTrue(text.indexOf("  signaling_mode:") < text.indexOf("  udp_port:"));
        assertTrue(text.indexOf("    certificate:") < text.indexOf("    private_key:"));
        assertTrue(text.indexOf("    private_key:") < text.indexOf("    password:"));
    }

    @Test
    public void sectionsGainNewOptions() throws Exception {
        File file = Files.createTempDirectory("waterdog-config").resolve("config.yml").toFile();
        Path path = file.toPath();
        Files.writeString(path, "nethernet:\n  enabled: false\n  udp_port: 19134\n");

        ProxyConfig config = new ProxyConfig(file);
        config.init();

        // A section already in the file is the one place Yamler will not add a key by itself
        String text = Files.readString(path);
        assertTrue(text.contains("\n  https:"), "an option added since the file was written is missing");
        assertTrue(text.contains("# Serves signaling over HTTPS as well as HTTP, on the same port"));
        assertEquals(19134, config.getNetherNetSettings().getUdpPort(), "an existing value was overwritten");
        assertFalse(config.getNetherNetSettings().enabled());
    }

    @Test
    public void udpPortZeroTakesThePortAfterTheListenerNoServerUses() throws Exception {
        File file = Files.createTempDirectory("waterdog-config").resolve("config.yml").toFile();
        ProxyConfig config = new ProxyConfig(file);
        config.init();

        NetherNetSettings settings = config.getNetherNetSettings();
        assertTrue(Files.readString(file.toPath()).contains("\n  udp_port: 0\n"), "0 is replaced in the file");
        // The example server, lobby1, is on 19133, the port after the listener's
        assertEquals(19134, settings.resolveUdpPort(19132, Set.of(19133)));
    }

    @Test
    public void udpPortKeepsAChosenPort() throws Exception {
        File file = Files.createTempDirectory("waterdog-config").resolve("config.yml").toFile();
        Files.writeString(file.toPath(), "nethernet:\n  udp_port: 19140\n");
        ProxyConfig config = new ProxyConfig(file);
        config.init();

        assertEquals(19140, config.getNetherNetSettings().resolveUdpPort(19132, Set.of(19133)));
    }

    @Test
    public void nestedSectionsRoundTrip() throws Exception {
        File file = Files.createTempDirectory("waterdog-config").resolve("config.yml").toFile();
        Path path = file.toPath();

        new ProxyConfig(file).save();
        Files.writeString(path, Files.readString(path)
                .replace("    certificate: ''", "    certificate: 'certs/fullchain.pem'")
                .replace("    private_key: ''", "    private_key: 'certs/privkey.pem'"));

        ProxyConfig config = new ProxyConfig(file);
        config.init();

        NetherNetSettings settings = config.getNetherNetSettings();
        HttpsSettings https = settings.getHttps();
        assertEquals("certs/fullchain.pem", https.getCertificate());
        assertEquals("certs/privkey.pem", https.getPrivateKey());
        assertEquals("", https.getPassword());
        assertTrue(https.enabled());
    }
}
