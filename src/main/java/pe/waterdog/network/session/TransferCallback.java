/*
 * Copyright 2021 WaterdogTEAM
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pe.waterdog.network.session;

import com.nukkitx.protocol.bedrock.BedrockClient;
import com.nukkitx.protocol.bedrock.BedrockClientSession;
import com.nukkitx.protocol.bedrock.handler.BatchHandler;
import com.nukkitx.protocol.bedrock.packet.SetLocalPlayerAsInitializedPacket;
import com.nukkitx.protocol.bedrock.packet.StopSoundPacket;
import pe.waterdog.event.defaults.TransferCompleteEvent;
import pe.waterdog.network.ServerInfo;
import pe.waterdog.network.bridge.TransferBatchBridge;
import pe.waterdog.network.rewrite.types.RewriteData;
import pe.waterdog.player.ProxiedPlayer;

public class TransferCallback {
    
    private final ProxiedPlayer player;
    private final BedrockClient client;
    private final ServerInfo targetServer;
    
    public TransferCallback(ProxiedPlayer player, BedrockClient client, ServerInfo targetServer) {
        this.player = player;
        this.client = client;
        this.targetServer = targetServer;
    }

    public BedrockClientSession getDownstream() {
        return this.client.getSession();
    }
    
    public void onTransferComplete() {        
        BatchHandler batchBridge = this.getDownstream().getBatchHandler();
        if (batchBridge instanceof TransferBatchBridge) {
            // Allow transfer queue to be sent
            ((TransferBatchBridge) batchBridge).setDimLockActive(false);
        }

        RewriteData rewriteData = this.player.getRewriteData();

        SetLocalPlayerAsInitializedPacket initializedPacket = new SetLocalPlayerAsInitializedPacket();
        initializedPacket.setRuntimeEntityId(rewriteData.getOriginalEntityId());
        this.getDownstream().sendPacket(initializedPacket);
        this.getDownstream().sendPacket(rewriteData.getChunkRadius());

        StopSoundPacket soundPacket = new StopSoundPacket();
        soundPacket.setSoundName("portal.travel");
        soundPacket.setStoppingAllSound(true);
        this.player.sendPacketImmediately(soundPacket);

        // PlayerRewriteUtils.injectPosition(this.player.getUpstream(), rewriteData.getSpawnPosition(), Vector3f.ZERO, rewriteData.getEntityId());

        ServerConnection oldServer = this.player.getServer();
        oldServer.getInfo().removePlayer(this.player);
        oldServer.disconnect();

        this.targetServer.addPlayer(this.player);
        this.player.setPendingConnection(null);

        ServerConnection server = new ServerConnection(this.client, this.getDownstream(), this.targetServer);
        SessionInjections.injectPostDownstreamHandlers(server, this.player);

        if (batchBridge instanceof TransferBatchBridge) {
            // Make sure queue will be sent
            ((TransferBatchBridge) batchBridge).flushQueue(this.getDownstream());
        }

        this.player.setServer(server);
        this.player.setAcceptPlayStatus(true);

        TransferCompleteEvent event = new TransferCompleteEvent(oldServer, server, this.player);
        this.player.getProxy().getEventManager().callEvent(event);
    }
}
