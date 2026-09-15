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
import org.cloudburstmc.netty.signaling.ServerStatus;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NetherNetCommandTest {

    @Test
    void parsesBase64UrlSnapshot() {
        String json = "{\"name\":\"Lobby\",\"protocol\":924,\"version\":\"1.26.50\",\"level\":\"hub\","
                + "\"players\":3,\"maxPlayers\":100,\"gameType\":1}";
        String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));

        ServerStatus status = NetherNetCommand.parseStatus(encoded);

        assertEquals(new ServerStatus("Lobby", 924, "1.26.50", "hub", 3, 100, 1), status);
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
}
