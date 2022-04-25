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

import com.nukkitx.protocol.bedrock.packet.SetLocalPlayerAsInitializedPacket;
import com.nukkitx.protocol.bedrock.packet.StopSoundPacket;
import dev.waterdog.waterdogpe.event.defaults.TransferCompleteEvent;
import dev.waterdog.waterdogpe.network.bridge.UpstreamBridge;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo;
import dev.waterdog.waterdogpe.network.rewrite.types.RewriteData;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import dev.waterdog.waterdogpe.utils.types.TranslationContainer;

import static dev.waterdog.waterdogpe.player.PlayerRewriteUtils.*;

public class TransferCallback {

    public static final int TRANSFER_RESET = 0;
    public static final int TRANSFER_PHASE_1 = 1;
    public static final int TRANSFER_PHASE_2 = 2;

    private final ProxiedPlayer player;
    private final DownstreamClient client;
    private final ServerInfo targetServer;
    private final int targetDimension;

    public TransferCallback(ProxiedPlayer player, DownstreamClient client, int targetDimension) {
        this.player = player;
        this.client = client;
        this.targetServer = client.getServerInfo();
        this.targetDimension = targetDimension;
    }

    public DownstreamSession getDownstream() {
        return this.client.getSession();
    }

    public boolean onDimChangeSuccess() {
        int dimChangeState = this.player.getDimensionChangeState();
        switch (dimChangeState) {
            case TRANSFER_PHASE_1:
                // First dimension change was completed successfully.
                this.onTransferPhase1Completed();
                this.player.setDimensionChangeState(TRANSFER_PHASE_2);
                break;
            case TRANSFER_PHASE_2:
                // At this point dimension change sequence was completed.
                // We can finally fully initialize connection.
                this.onTransferPhase2Completed();
                this.player.setDimensionChangeState(TRANSFER_RESET);
                break;
            default:
                return false;
        }
        return true;
    }

    private void onTransferPhase1Completed() {
        RewriteData rewriteData = this.player.getRewriteData();
        injectEntityImmobile(this.player.getUpstream(), rewriteData.getEntityId(), true);

        if (rewriteData.getDimension() != this.targetDimension) {
            // Send second dim-change to correct dimension
            rewriteData.setDimension(determineDimensionId(rewriteData.getDimension(), this.targetDimension));
            injectDimensionChange(this.player.getUpstream(), rewriteData.getDimension(), rewriteData.getSpawnPosition(), this.player.getProtocol());
        }

        injectRemoveAllEffects(this.player.getUpstream(), rewriteData.getEntityId());
        injectClearWeather(this.player.getUpstream());
        injectGameRules(this.player.getUpstream(), rewriteData.getGameRules());
    }

    private void onTransferPhase2Completed() {
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

        this.client.getSession().onTransferCompleted(this.player, this::onTransferComplete0);
    }

    private void onTransferComplete0() {
        this.player.getUpstream().setBatchHandler(this.client.newUpstreamBridge(this.player));

        DownstreamClient oldDownstream = this.player.getDownstream();
        this.player.setPendingConnection(null);
        this.player.setDownstream(this.client);
        this.targetServer.addPlayer(this.player);
        this.player.setAcceptPlayStatus(true);

        TransferCompleteEvent event = new TransferCompleteEvent(oldDownstream, this.client, this.player);
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
