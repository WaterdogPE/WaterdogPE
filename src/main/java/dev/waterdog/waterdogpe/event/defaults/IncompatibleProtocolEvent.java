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

package dev.waterdog.waterdogpe.event.defaults;

import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.protocol.bedrock.packet.PlayStatusPacket;

/**
 * This event is called when a player with an incompatible version is attempting to connect.
 * {@link IncompatibleProtocolEvent#status} can be set to {@link PlayStatusPacket.Status#LOGIN_SUCCESS}
 * and {@link IncompatibleProtocolEvent#disconnectMessage} can be modified to show a custom message instead of client's
 * default incompatible version message. Which is sometimes confusing to players.
 * <p>
 * This event cannot be canceled, the player will disconnect regardless of {@link IncompatibleProtocolEvent#status}.
 */
public class IncompatibleProtocolEvent extends PlayerEvent {

    @Getter
    private final int protocolVersion;
    @Getter
    @Setter
    private PlayStatusPacket.Status status;
    @Getter
    @Setter
    private CharSequence disconnectMessage;

    public IncompatibleProtocolEvent(ProxiedPlayer player, int protocolVersion, PlayStatusPacket.Status status, CharSequence disconnectMessage) {
        super(player);
        this.protocolVersion = protocolVersion;
        this.status = status;
        this.disconnectMessage = disconnectMessage;
    }
}

