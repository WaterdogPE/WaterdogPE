/*
 * Copyright 2022 WaterdogTEAM
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

package dev.waterdog.waterdogpe.event.defaults;

import dev.waterdog.waterdogpe.event.AsyncEvent;
import dev.waterdog.waterdogpe.event.CancellableEvent;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import org.cloudburstmc.protocol.bedrock.packet.PacketViolationWarningPacket;

/**
 * Called right before the initial connect is made.
 * Cancelling it will close the connection with the cancelReason as the disconnectMessage.
 */
@AsyncEvent
public class PlayerPacketViolationEvent extends PlayerEvent {
    private PacketViolationWarningPacket packetViolationWarningPacket;

    public PlayerPacketViolationEvent(ProxiedPlayer player, PacketViolationWarningPacket pk) {
        super(player);
        this.packetViolationWarningPacket = pk;
    }

    public PacketViolationWarningPacket getPacketViolationWarningPacket() {
        return packetViolationWarningPacket;
    }
}
