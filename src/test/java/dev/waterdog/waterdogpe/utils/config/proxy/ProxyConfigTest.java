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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
