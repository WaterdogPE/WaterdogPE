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

import dev.waterdog.waterdogpe.utils.config.proxy.NetherNetSettings;
import dev.waterdog.waterdogpe.utils.config.proxy.ProxyConfig;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NetherNetPortTest {

    private static ProxyConfig config(String text) throws Exception {
        File file = Files.createTempDirectory("waterdog-config").resolve("config.yml").toFile();
        Files.writeString(file.toPath(), text);
        ProxyConfig config = new ProxyConfig(file);
        config.init();
        return config;
    }

    @Test
    public void eachListenerDerivesAPortNoListenerOrServerUses() throws Exception {
        // The example server, lobby1, is on 19133, and the listener on 19132
        ProxyConfig config = config("listener:\n  additional_ports: [19134]\n");
        NetherNetSettings settings = config.getNetherNetSettings();

        int primary = settings.resolveWebrtcPort(19132, true, NetherNetInterface.takenPorts(config, List.of()));
        int additional = settings.resolveWebrtcPort(19134, true, NetherNetInterface.takenPorts(config, List.of(primary)));

        assertEquals(19135, primary);
        assertEquals(19136, additional);
    }
}
