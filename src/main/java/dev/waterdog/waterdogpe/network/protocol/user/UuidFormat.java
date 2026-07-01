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

import java.util.UUID;

/**
 * Determines how the unique id (UUID) of a connecting player is computed.
 */
public enum UuidFormat {

    /**
     * Uses the UUID parsed from the player's identity (login JWT chain). This is the default behaviour.
     */
    IDENTITY,

    /**
     * Derives the UUID from the player's XUID, identical to GeyserMC/Floodgate.
     * The XUID is parsed as a long and stored in the lower 64 bits of the UUID, leaving the upper bits zeroed,
     * which results in a zero-padded hex representation of the XUID.
     */
    FLOODGATE;

    /**
     * Resolves the UUID that should be assigned to the player based on this format.
     * If the format can not be applied (e.g. FLOODGATE with a missing or non-numeric XUID),
     * the identity UUID is returned as a fallback.
     *
     * @param identity the UUID parsed from the player's identity chain.
     * @param xuid     the player's XUID.
     * @return the UUID to use for the player.
     */
    public UUID resolve(UUID identity, String xuid) {
        if (this != FLOODGATE || xuid == null || xuid.isEmpty()) {
            return identity;
        }
        try {
            return new UUID(0, Long.parseLong(xuid));
        } catch (NumberFormatException e) {
            return identity;
        }
    }
}
