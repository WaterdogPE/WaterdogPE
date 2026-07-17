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

package dev.waterdog.waterdogpe.transfer;

import dev.waterdog.waterdogpe.network.protocol.rewrite.types.StartGameSettings;
import org.cloudburstmc.protocol.bedrock.data.AuthoritativeMovementMode;
import org.cloudburstmc.protocol.bedrock.packet.StartGamePacket;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StartGameSettingsTest {

    private static StartGamePacket packet(AuthoritativeMovementMode movementMode, int rewindHistorySize,
                                          boolean blockBreaking, boolean inventories, boolean idsHashed) {
        StartGamePacket packet = new StartGamePacket();
        packet.setAuthoritativeMovementMode(movementMode);
        packet.setRewindHistorySize(rewindHistorySize);
        packet.setServerAuthoritativeBlockBreaking(blockBreaking);
        packet.setInventoriesServerAuthoritative(inventories);
        packet.setBlockNetworkIdsHashed(idsHashed);
        return packet;
    }

    private static StartGamePacket basePacket() {
        return packet(AuthoritativeMovementMode.SERVER, 20, true, true, true);
    }

    @Test
    void identicalSettingsAreCompatible() {
        StartGameSettings settings = StartGameSettings.from(basePacket());
        assertNull(settings.findIncompatibilities(basePacket()));
    }

    @Test
    void capturesPacketFields() {
        StartGameSettings settings = StartGameSettings.from(basePacket());
        assertEquals(AuthoritativeMovementMode.SERVER, settings.movementMode());
        assertEquals(20, settings.rewindHistorySize());
        assertTrue(settings.serverAuthoritativeBlockBreaking());
        assertTrue(settings.inventoriesServerAuthoritative());
        assertTrue(settings.blockNetworkIdsHashed());
    }

    @Test
    void detectsMovementModeMismatch() {
        StartGameSettings settings = StartGameSettings.from(basePacket());
        StartGamePacket other = basePacket();
        other.setAuthoritativeMovementMode(AuthoritativeMovementMode.CLIENT);
        String result = settings.findIncompatibilities(other);
        assertNotNull(result);
        assertTrue(result.contains("authoritativeMovementMode"));
    }

    @Test
    void detectsRewindHistoryMismatch() {
        StartGameSettings settings = StartGameSettings.from(basePacket());
        StartGamePacket other = basePacket();
        other.setRewindHistorySize(0);
        String result = settings.findIncompatibilities(other);
        assertNotNull(result);
        assertTrue(result.contains("rewindHistorySize"));
    }

    @Test
    void detectsBlockBreakingMismatch() {
        StartGameSettings settings = StartGameSettings.from(basePacket());
        StartGamePacket other = basePacket();
        other.setServerAuthoritativeBlockBreaking(false);
        String result = settings.findIncompatibilities(other);
        assertNotNull(result);
        assertTrue(result.contains("serverAuthoritativeBlockBreaking"));
    }

    @Test
    void detectsInventoryAuthorityMismatch() {
        StartGameSettings settings = StartGameSettings.from(basePacket());
        StartGamePacket other = basePacket();
        other.setInventoriesServerAuthoritative(false);
        String result = settings.findIncompatibilities(other);
        assertNotNull(result);
        assertTrue(result.contains("inventoriesServerAuthoritative"));
    }

    @Test
    void detectsHashedIdsMismatch() {
        StartGameSettings settings = StartGameSettings.from(basePacket());
        StartGamePacket other = basePacket();
        other.setBlockNetworkIdsHashed(false);
        String result = settings.findIncompatibilities(other);
        assertNotNull(result);
        assertTrue(result.contains("blockNetworkIdsHashed"));
    }

    @Test
    void listsAllMismatches() {
        StartGameSettings settings = StartGameSettings.from(basePacket());
        StartGamePacket other = basePacket();
        other.setAuthoritativeMovementMode(AuthoritativeMovementMode.CLIENT);
        other.setRewindHistorySize(0);
        other.setBlockNetworkIdsHashed(false);
        String result = settings.findIncompatibilities(other);
        assertNotNull(result);
        assertTrue(result.contains("authoritativeMovementMode"));
        assertTrue(result.contains("rewindHistorySize"));
        assertTrue(result.contains("blockNetworkIdsHashed"));
    }
}
