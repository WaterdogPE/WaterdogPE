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

package dev.waterdog.waterdogpe.network.session;

import com.nukkitx.network.util.DisconnectReason;
import com.nukkitx.protocol.bedrock.BedrockSession;
import com.nukkitx.protocol.bedrock.packet.RequestNetworkSettingsPacket;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;

public class SessionInjections {

    public static void injectUpstreamSettings(BedrockSession upstream, ProxiedPlayer player) {
        upstream.setCompressionLevel(player.getProxy().getConfiguration().getUpstreamCompression());
        upstream.addDisconnectHandler(reason -> player.disconnect(reason.name()));
    }

    public static void injectNewDownstream(ProxiedPlayer player, DownstreamSession downstream, DownstreamClient client) {
        downstream.addDisconnectHandler((reason) -> {
            if (client.getSession().equals(downstream)) {
                // Make sure everything is closed as excepted
                client.close();
            }

            player.getLogger().info("[" + player.getAddress() + "|" + player.getName() + "] -> Downstream [" + client.getServerInfo().getServerName() + "] has disconnected");
            if (reason == DisconnectReason.TIMED_OUT) {
                player.onDownstreamTimeout();
            }
        });
    }

    public static void requestNetworkSettings(ProxiedPlayer player, DownstreamSession downstream) {
        RequestNetworkSettingsPacket packet = new RequestNetworkSettingsPacket();
        packet.setProtocolVersion(player.getProtocol().getProtocol());
        downstream.sendPacketImmediately(packet);
    }
}
