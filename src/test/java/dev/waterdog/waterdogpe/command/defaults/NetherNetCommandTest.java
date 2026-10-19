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

package dev.waterdog.waterdogpe.command.defaults;

import dev.waterdog.waterdogpe.network.nethernet.NetherNetProvider;
import org.cloudburstmc.netty.signaling.ProviderClient;
import org.cloudburstmc.netty.signaling.ServerStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NetherNetCommandTest {

    @Test
    void parsesBase64UrlSnapshot() {
        String json = "{\"name\":\"Lobby\",\"protocol\":924,\"version\":\"1.26.50\",\"level\":\"hub\","
                + "\"maxPlayerCount\":100,\"gameType\":1}";
        String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));

        ServerStatus status = NetherNetCommand.parseStatus(encoded);

        assertEquals(new ServerStatus("Lobby", "hub", 100, 1), status);
    }

    @Test
    void rejectsGarbage() {
        assertThrows(RuntimeException.class, () -> NetherNetCommand.parseStatus("not base64url!"));
        assertThrows(RuntimeException.class, () -> NetherNetCommand.parseStatus(
                Base64.getUrlEncoder().encodeToString("null".getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void failureMessageUnwrapsFuturesButNotInternals() {
        Throwable wrapped = new CompletionException(new IllegalStateException("secret detail"));
        assertEquals("IllegalStateException", NetherNetProvider.failureMessage(wrapped));
    }

    @Test
    void theOperatorLinkGoesToAFileOnlyThisUserReads(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("operator-link.txt");
        var action = new ProviderClient.OperatorAction(URI.create("https://panel.example.net/claim/secret"), "Claim it.",
                1_800_000_000_000L);
        assertEquals("Claim it. The link is in " + file + ".", NetherNetProvider.storeOperatorAction(file, Optional.of(action)));
        assertEquals("Claim it.\nhttps://panel.example.net/claim/secret\nExpires 2027-01-15T08:00:00Z\n", Files.readString(file));
        if (file.getFileSystem().supportedFileAttributeViews().contains("posix")) {
            assertEquals(PosixFilePermissions.fromString("rw-------"), Files.getPosixFilePermissions(file));
        }
        // Once the service offers none, as after a claim, the old link goes
        assertNull(NetherNetProvider.storeOperatorAction(file, Optional.empty()));
        assertFalse(Files.exists(file));
    }
}
