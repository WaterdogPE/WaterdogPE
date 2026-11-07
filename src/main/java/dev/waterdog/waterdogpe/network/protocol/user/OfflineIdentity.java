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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Supplies the stable fallback BDS uses for proxy-signed, offline-mode logins. */
public final class OfflineIdentity {

    private static final String NAMESPACE = "waterdogpe-offline-self-signed:";

    private OfflineIdentity() {
    }

    public static boolean stabilize(JsonObject clientData, String xuid, boolean xboxAuthed) {
        if (!xboxAuthed || xuid == null || xuid.isBlank()) {
            return false;
        }

        JsonElement existing = clientData.get("SelfSignedId");
        if (existing != null && !existing.isJsonNull()) {
            if (!existing.isJsonPrimitive() || !existing.getAsJsonPrimitive().isString()
                    || !existing.getAsString().isBlank()) {
                return false;
            }
        }

        UUID stableId = UUID.nameUUIDFromBytes((NAMESPACE + xuid).getBytes(StandardCharsets.UTF_8));
        clientData.addProperty("SelfSignedId", stableId.toString());
        return true;
    }
}
