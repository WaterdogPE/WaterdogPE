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

package dev.waterdog.waterdogpe.network.protocol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProtocolVersionTest {

    /**
     * isBefore/isAfter compare protocolInternal, so declaration order must match version order.
     * Catches a misplaced enum entry and a bad internalPatch value alike.
     */
    @Test
    void declarationOrderIsStrictlyIncreasing() {
        ProtocolVersion[] versions = ProtocolVersion.values();
        for (int i = 1; i < versions.length; i++) {
            ProtocolVersion previous = versions[i - 1];
            ProtocolVersion current = versions[i];
            assertTrue(previous.isBefore(current),
                    previous + " (" + previous.getProtocolInternal() + ") must sort before "
                            + current + " (" + current.getProtocolInternal() + ")");
        }
    }

    @Test
    void latestAndOldestMatchTheOrdering() {
        for (ProtocolVersion version : ProtocolVersion.values()) {
            assertTrue(version.isBeforeOrEqual(ProtocolVersion.latest()), version + " sorts after latest()");
            assertTrue(version.isAfterOrEqual(ProtocolVersion.oldest()), version + " sorts before oldest()");
        }
    }

    /**
     * Versions that shipped wire changes without a client side protocol bump share a protocol
     * number with their base version, and are ordered by the patch index instead.
     */
    @Test
    void unbumpedVersionsSortBetweenTheirNeighbors() {
        assertEquals(ProtocolVersion.MINECRAFT_PE_1_19_60.getProtocol(),
                ProtocolVersion.MINECRAFT_PE_1_19_62.getProtocol());
        assertTrue(ProtocolVersion.MINECRAFT_PE_1_19_60.isBefore(ProtocolVersion.MINECRAFT_PE_1_19_62));
        assertTrue(ProtocolVersion.MINECRAFT_PE_1_19_62.isBefore(ProtocolVersion.MINECRAFT_PE_1_19_63));

        assertEquals(ProtocolVersion.MINECRAFT_PE_1_26_40.getProtocol(),
                ProtocolVersion.MINECRAFT_PE_1_26_44.getProtocol());
        assertTrue(ProtocolVersion.MINECRAFT_PE_1_26_40.isBefore(ProtocolVersion.MINECRAFT_PE_1_26_44));
        assertTrue(ProtocolVersion.MINECRAFT_PE_1_26_44.isBefore(ProtocolVersion.MINECRAFT_PE_1_26_45));
    }
}
