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

package dev.waterdog.network.session;

import com.google.common.base.Preconditions;
import com.nukkitx.network.util.DisconnectReason;
import com.nukkitx.protocol.bedrock.BedrockClient;
import com.nukkitx.protocol.bedrock.BedrockSession;
import dev.waterdog.network.ServerInfo;
import dev.waterdog.network.bridge.DownstreamBridge;
import dev.waterdog.network.bridge.UpstreamBridge;
import dev.waterdog.network.downstream.ConnectedDownstreamHandler;
import dev.waterdog.player.ProxiedPlayer;

public class SessionInjections {

    public static void injectUpstreamSettings(BedrockSession upstream, ProxiedPlayer player) {
        upstream.setCompressionLevel(player.getProxy().getConfiguration().getUpstreamCompression());
        upstream.addDisconnectHandler(reason -> player.disconnect());
    }

    public static void injectNewDownstream(BedrockSession downstream, ProxiedPlayer player, ServerInfo server, BedrockClient downstreamClient) {
        downstream.setCompressionLevel(player.getProxy().getConfiguration().getDownstreamCompression());
        downstream.addDisconnectHandler((reason) -> {
            if (downstreamClient != null && downstreamClient.getSession().equals(downstream)) {
                // Make sure everything is closed as excepted.
                downstreamClient.close();
            }

            player.getLogger().info("[" + player.getAddress() + "|" + player.getName() + "] -> Downstream [" + server.getServerName() + "] has disconnected");
            if (reason == DisconnectReason.TIMED_OUT) {
                player.onDownstreamTimeout();
            }
        });
    }

    public static void injectInitialHandlers(ServerConnection server, ProxiedPlayer player) {
        Preconditions.checkArgument(server != null && player != null, "Player and ServerConnection can not be null!");
        int blockingId = server.getDownstream().getHardcodedBlockingId().get();
        player.getUpstream().getHardcodedBlockingId().set(blockingId);
        server.getDownstream().setPacketHandler(new ConnectedDownstreamHandler(player, server));
    }

    public static void injectDownstreamHandlers(ServerConnection server, ProxiedPlayer player) {
        injectInitialHandlers(server, player);
        player.getUpstream().setBatchHandler(new UpstreamBridge(player, server.getDownstream()));
        server.getDownstream().setBatchHandler(new DownstreamBridge(player, player.getUpstream()));
    }
}
