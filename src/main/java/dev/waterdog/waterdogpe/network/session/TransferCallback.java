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

import com.nukkitx.network.raknet.RakNetSession;
import com.nukkitx.protocol.bedrock.BedrockClient;
import com.nukkitx.protocol.bedrock.BedrockClientSession;
import com.nukkitx.protocol.bedrock.packet.SetLocalPlayerAsInitializedPacket;
import com.nukkitx.protocol.bedrock.packet.StopSoundPacket;
import dev.waterdog.waterdogpe.event.defaults.TransferCompleteEvent;
import dev.waterdog.waterdogpe.network.ServerInfo;
import dev.waterdog.waterdogpe.network.bridge.TransferBatchBridge;
import dev.waterdog.waterdogpe.network.rewrite.types.RewriteData;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import dev.waterdog.waterdogpe.utils.exceptions.CancelSignalException;
import dev.waterdog.waterdogpe.utils.types.TranslationContainer;

import static dev.waterdog.waterdogpe.player.PlayerRewriteUtils.*;

public class TransferCallback {

    public static final int TRANSFER_RESET = 0;
    public static final int TRANSFER_PHASE_1 = 1;
    public static final int TRANSFER_PHASE_2 = 2;

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

    public TransferBatchBridge getBatchBridge() {
        if (this.getDownstream().getBatchHandler() instanceof TransferBatchBridge) {
            return  (TransferBatchBridge) this.getDownstream().getBatchHandler();
        }
        return null;
    }

    public boolean onDimChangeSuccess() {
        int dimChangeState = this.player.getDimensionChangeState();
        switch (dimChangeState) {
            case TRANSFER_PHASE_1:
                // First dimension change was completed successfully.
                this.onTransferAccepted();
                this.player.setDimensionChangeState(TRANSFER_PHASE_2);
                break;
            case TRANSFER_PHASE_2:
                // At this point dimension change sequence was completed.
                // We can finally fully initialize connection.
                this.onTransferComplete();
                this.player.setDimensionChangeState(TRANSFER_RESET);
                break;
            default:
                return false;
        }
        return true;
    }

    private void onTransferAccepted() {
        RewriteData rewriteData = this.player.getRewriteData();
        injectEntityImmobile(this.player.getUpstream(), rewriteData.getEntityId(), true);

        rewriteData.setDimension(determineDimensionId(rewriteData.getDimension()));
        injectDimensionChange(this.player.getUpstream(), rewriteData.getDimension(), rewriteData.getSpawnPosition());

        injectRemoveAllEffects(this.player.getUpstream(), rewriteData.getEntityId());
        injectClearWeather(this.player.getUpstream());
        injectGameRules(this.player.getUpstream(), rewriteData.getGameRules());
    }

    private void onTransferComplete() {
        RewriteData rewriteData = this.player.getRewriteData();
        rewriteData.setTransferCallback(null);

        StopSoundPacket soundPacket = new StopSoundPacket();
        soundPacket.setSoundName("portal.travel");
        soundPacket.setStoppingAllSound(true);
        this.player.sendPacketImmediately(soundPacket);

        if (this.client.getSession() == null || this.getDownstream().isClosed()) {
            this.onTransferFailed();
            return;
        }

        SetLocalPlayerAsInitializedPacket initializedPacket = new SetLocalPlayerAsInitializedPacket();
        initializedPacket.setRuntimeEntityId(this.player.getRewriteData().getOriginalEntityId());
        this.getDownstream().sendPacket(initializedPacket);

        // Allow transfer queue to be sent
        TransferBatchBridge batchBridge = this.getBatchBridge();
        if (batchBridge != null) {
            batchBridge.setDimLockActive(false);
        }

        // Change downstream bridge on same eventLoop as packet are being processed on to
        // prevent packet reordering in some situations
        RakNetSession rakSession = (RakNetSession) this.getDownstream().getConnection();
        if (rakSession.getEventLoop().inEventLoop()) {
            this.onTransferComplete0();
        } else {
            rakSession.getEventLoop().execute(this::onTransferComplete0);
        }
    }

    private void onTransferComplete0() {
        TransferBatchBridge batchBridge = this.getBatchBridge();
        ServerConnection server = new ServerConnection(this.client, this.getDownstream(), this.targetServer);
        SessionInjections.injectPostDownstreamHandlers(server, this.player);

        // Flush the queue last time before any other packets are received
        if (batchBridge != null) {
            batchBridge.flushQueue(this.getDownstream());
        }

        ServerConnection oldServer = this.player.getServer();
        this.player.setPendingConnection(null);
        this.player.setServer(server);
        this.targetServer.addPlayer(this.player);
        this.player.setAcceptPlayStatus(true);

        TransferCompleteEvent event = new TransferCompleteEvent(oldServer, server, this.player);
        this.player.getProxy().getEventManager().callEvent(event);
    }

    public void onTransferFailed() {
        if (this.player.sendToFallback(this.targetServer, "Transfer failed")) {
            this.player.sendMessage(new TranslationContainer("waterdog.connected.fallback", this.targetServer.getServerName()));
        } else {
            this.player.disconnect(new TranslationContainer("waterdog.downstream.transfer.failed", targetServer.getServerName(), "Server was closed"));
        }

        this.client.close();
        this.player.getLogger().warning("Failed to transfer " + this.player.getName() + " to " + this.targetServer.getServerName() + ": Server was closed");
    }
}
