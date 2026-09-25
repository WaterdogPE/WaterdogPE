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

package dev.waterdog.waterdogpe.network.protocol.rewrite.types;

import org.cloudburstmc.protocol.bedrock.data.AuthoritativeMovementMode;
import org.cloudburstmc.protocol.bedrock.packet.StartGamePacket;

import java.util.StringJoiner;

/**
 * StartGame settings the client locks in on its first spawn and cannot change on later transfers.
 */
public record StartGameSettings(AuthoritativeMovementMode movementMode,
                                int rewindHistorySize,
                                boolean serverAuthoritativeBlockBreaking,
                                boolean inventoriesServerAuthoritative,
                                boolean blockNetworkIdsHashed) {

    public static StartGameSettings from(StartGamePacket packet) {
        return new StartGameSettings(packet.getAuthoritativeMovementMode(),
                packet.getRewindHistorySize(),
                packet.isServerAuthoritativeBlockBreaking(),
                packet.isInventoriesServerAuthoritative(),
                packet.isBlockNetworkIdsHashed());
    }

    /**
     * The rewind history size a Bedrock Dedicated Server reports, and the one every downstream is
     * made to report along with it.
     */
    private static final int BEDROCK_REWIND_HISTORY_SIZE = 40;

    /**
     * Reports the Bedrock Dedicated Server rewind history size on a StartGame packet a downstream
     * server sent.
     *
     * <p>Server software disagrees on this value and the client can only hold one: it locks what the
     * first server reports and a later server that disagrees is refused. A Bedrock Dedicated Server
     * reports 40 and no longer has a setting to change that, so it is the value every server has to
     * agree on. Nothing else in the packet is touched.</p>
     */
    public static void applyBedrockRewindHistory(StartGamePacket packet) {
        packet.setRewindHistorySize(BEDROCK_REWIND_HISTORY_SIZE);
    }

    /**
     * @return a description of the incompatible settings, or null if the packet is compatible.
     */
    public String findIncompatibilities(StartGamePacket packet) {
        StringJoiner mismatches = new StringJoiner(", ");
        if (this.movementMode != packet.getAuthoritativeMovementMode()) {
            mismatches.add("authoritativeMovementMode " + this.movementMode + " != " + packet.getAuthoritativeMovementMode());
        }
        if (this.rewindHistorySize != packet.getRewindHistorySize()) {
            mismatches.add("rewindHistorySize " + this.rewindHistorySize + " != " + packet.getRewindHistorySize());
        }
        if (this.serverAuthoritativeBlockBreaking != packet.isServerAuthoritativeBlockBreaking()) {
            mismatches.add("serverAuthoritativeBlockBreaking " + this.serverAuthoritativeBlockBreaking + " != " + packet.isServerAuthoritativeBlockBreaking());
        }
        if (this.inventoriesServerAuthoritative != packet.isInventoriesServerAuthoritative()) {
            mismatches.add("inventoriesServerAuthoritative " + this.inventoriesServerAuthoritative + " != " + packet.isInventoriesServerAuthoritative());
        }
        if (this.blockNetworkIdsHashed != packet.isBlockNetworkIdsHashed()) {
            mismatches.add("blockNetworkIdsHashed " + this.blockNetworkIdsHashed + " != " + packet.isBlockNetworkIdsHashed());
        }
        return mismatches.length() == 0 ? null : mismatches.toString();
    }
}