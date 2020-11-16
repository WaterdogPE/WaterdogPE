/**
 * Copyright 2020 WaterdogTEAM
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pe.waterdog.network.session;

import com.google.common.base.Preconditions;
import com.nukkitx.network.util.DisconnectReason;
import com.nukkitx.protocol.bedrock.BedrockPacket;
import com.nukkitx.protocol.bedrock.BedrockSession;
import com.nukkitx.protocol.bedrock.handler.BatchHandler;
import pe.waterdog.network.ServerInfo;
import pe.waterdog.network.bridge.DownstreamBridge;
import pe.waterdog.network.bridge.TransferBatchBridge;
import pe.waterdog.network.bridge.UpstreamBridge;
import pe.waterdog.network.downstream.ConnectedDownstreamHandler;
import pe.waterdog.network.upstream.UpstreamHandler;
import pe.waterdog.player.ProxiedPlayer;

import java.util.Queue;

public class SessionInjections {

    public static void injectUpstreamHandlers(BedrockSession upstream, ProxiedPlayer player) {
        upstream.setCompressionLevel(player.getProxy().getConfiguration().getUpstreamCompression());
        upstream.setPacketHandler(new UpstreamHandler(player));
        upstream.addDisconnectHandler((reason) -> player.disconnect((String) null));
    }

    public static void injectNewDownstream(BedrockSession downstream, ProxiedPlayer player, ServerInfo server) {
        downstream.setCompressionLevel(player.getProxy().getConfiguration().getDownstreamCompression());
        downstream.addDisconnectHandler((reason) -> {
            player.getLogger().info("[" + downstream.getAddress() + "|" + player.getName() + "] -> Downstream [" + server.getServerName() + "] has disconnected");
            if (reason == DisconnectReason.TIMED_OUT) {
                player.onDownstreamTimeout();
            }
        });
    }

    public static void injectDownstreamHandlers(ServerConnection server, ProxiedPlayer player) {
        Preconditions.checkArgument(server != null && player != null, "Player and ServerConnection can not be null!");
        BatchHandler transferHandler = server.getDownstream().getBatchHandler();
        Queue<BedrockPacket> packetQueue = null;
        if (transferHandler instanceof TransferBatchBridge){
            packetQueue = ((TransferBatchBridge) transferHandler).getPacketQueue();
        }

        player.getUpstream().setBatchHandler(new UpstreamBridge(player, server.getDownstream()));
        server.getDownstream().setBatchHandler(new DownstreamBridge(player, player.getUpstream()));
        server.getDownstream().setPacketHandler(new ConnectedDownstreamHandler(player, server));

        // Send queued packets from downstream
        if (packetQueue != null && !packetQueue.isEmpty()){
            player.getUpstream().sendWrapped(packetQueue, player.getUpstream().isEncrypted());
        }
    }
}
