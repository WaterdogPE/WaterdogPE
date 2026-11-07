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

package dev.waterdog.waterdogpe.network.protocol.user;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfflineIdentityTest {

    @Test
    void authenticatedXuidProducesTheSameIdentityOnEveryLogin() {
        JsonObject first = new JsonObject();
        JsonObject second = new JsonObject();

        assertTrue(OfflineIdentity.stabilize(first, "2535000000000000", true));
        assertTrue(OfflineIdentity.stabilize(second, "2535000000000000", true));
        assertEquals(first.get("SelfSignedId"), second.get("SelfSignedId"));
    }

    @Test
    void differentXuidsProduceDifferentIdentities() {
        JsonObject first = new JsonObject();
        JsonObject second = new JsonObject();

        OfflineIdentity.stabilize(first, "2535000000000000", true);
        OfflineIdentity.stabilize(second, "2535000000000001", true);
        assertNotEquals(first.get("SelfSignedId"), second.get("SelfSignedId"));
    }

    @Test
    void preservesAClientSuppliedIdentity() {
        JsonObject data = new JsonObject();
        data.addProperty("SelfSignedId", "client-owned-id");

        assertFalse(OfflineIdentity.stabilize(data, "2535000000000000", true));
        assertEquals("client-owned-id", data.get("SelfSignedId").getAsString());
    }

    @Test
    void refusesAnUnauthenticatedOrBlankIdentity() {
        JsonObject unauthenticated = new JsonObject();
        JsonObject blank = new JsonObject();

        assertFalse(OfflineIdentity.stabilize(unauthenticated, "2535000000000000", false));
        assertFalse(OfflineIdentity.stabilize(blank, "", true));
        assertFalse(unauthenticated.has("SelfSignedId"));
        assertFalse(blank.has("SelfSignedId"));
    }
}
