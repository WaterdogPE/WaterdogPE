/*
 * Copyright 2021 WaterdogTEAM
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

package dev.waterdog.event.defaults;

import com.nukkitx.protocol.bedrock.packet.ResourcePacksInfoPacket;
import dev.waterdog.event.CancellableEvent;
import dev.waterdog.player.ProxiedPlayer;

/**
 * Called before the ResourcePacksInfoPacket is sent to player.
 * It is possible to cancel sending this packet or use custom instance of this packet.
 * WARNING: Modifying packet passed from construction will modify the packet for all other players!
 */
public class PlayerResourcePackInfoSendEvent extends PlayerEvent implements CancellableEvent {

    private ResourcePacksInfoPacket packet;

    public PlayerResourcePackInfoSendEvent(ProxiedPlayer player, ResourcePacksInfoPacket packet) {
        super(player);
        this.packet = packet;
    }

    public ResourcePacksInfoPacket getPacket() {
        return this.packet;
    }

    public void setPacket(ResourcePacksInfoPacket packet) {
        this.packet = packet;
    }
}
