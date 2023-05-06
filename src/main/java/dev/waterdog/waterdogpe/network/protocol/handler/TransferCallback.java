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

package dev.waterdog.waterdogpe.network.protocol.handler;

import dev.waterdog.waterdogpe.event.defaults.TransferCompleteEvent;
import dev.waterdog.waterdogpe.network.connection.client.ClientConnection;
import dev.waterdog.waterdogpe.network.protocol.handler.downstream.ConnectedDownstreamHandler;
import dev.waterdog.waterdogpe.network.protocol.handler.upstream.ConnectedUpstreamHandler;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo;
import dev.waterdog.waterdogpe.network.protocol.rewrite.types.RewriteData;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import dev.waterdog.waterdogpe.utils.types.TranslationContainer;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.packet.SetLocalPlayerAsInitializedPacket;
import org.cloudburstmc.protocol.bedrock.packet.StopSoundPacket;

import static dev.waterdog.waterdogpe.network.protocol.user.PlayerRewriteUtils.*;
import static dev.waterdog.waterdogpe.network.protocol.handler.TransferCallback.TransferPhase.*;

public class TransferCallback {
    public enum TransferPhase {
        RESET,
        PHASE_1,
        PHASE_2;
    }

    private final ProxiedPlayer player;
    private final ClientConnection connection;
    private final ServerInfo targetServer;
    private final ServerInfo sourceServer;
    private final int targetDimension;

    private volatile TransferPhase transferPhase = PHASE_1;

    public TransferCallback(ProxiedPlayer player, ClientConnection connection, ServerInfo sourceServer, int targetDimension) {
        this.player = player;
        this.connection = connection;
        this.targetServer = connection.getServerInfo();
        this.sourceServer = sourceServer;
        this.targetDimension = targetDimension;
    }

    public boolean onDimChangeSuccess() {
        switch (this.transferPhase) {
            case PHASE_1 -> this.onTransferPhase1Completed(); // First dimension change was completed successfully.
            case PHASE_2 -> this.onTransferPhase2Completed(); // At this point dimension change sequence was completed. We can finally fully initialize connection.
            default -> {
                return false;
            }
        }
        return true;
    }

    public void onTransferPhase1Completed() {
        if (this.transferPhase != PHASE_1) {
            return;
        }
        this.transferPhase = PHASE_2;

        RewriteData rewriteData = this.player.getRewriteData();
        injectEntityImmobile(this.player.getConnection(), rewriteData.getEntityId(), true);

        // Send second dim-change to correct dimension
        Vector3f spawnPosition = rewriteData.getSpawnPosition();
        injectPosition(this.player.getConnection(), spawnPosition, rewriteData.getRotation(), rewriteData.getEntityId());

        if (rewriteData.getDimension() != this.targetDimension) {
            rewriteData.setDimension(determineDimensionId(rewriteData.getDimension(), this.targetDimension));
            injectDimensionChange(this.player.getConnection(), rewriteData.getDimension(), rewriteData.getSpawnPosition(), rewriteData.getEntityId(), this.player.getProtocol(), false);
            injectChunkPublisherUpdate(this.player.getConnection(), spawnPosition.toInt(), Math.max(4, this.getChunkRadius()));

            StopSoundPacket soundPacket = new StopSoundPacket();
            soundPacket.setSoundName("*");
            soundPacket.setStoppingAllSound(true);
            this.player.sendPacketImmediately(soundPacket);
        }

        this.player.getConnection().setTransferQueueActive(false);
        if (this.player.getConnection().getPacketHandler() instanceof ConnectedUpstreamHandler handler) {
            handler.setTargetConnection(this.connection);
        }

        this.connection.setPacketHandler(new ConnectedDownstreamHandler(player, this.connection));
    }

    private void onTransferPhase2Completed() {
        if (this.transferPhase != PHASE_2) {
            return;
        }
        this.transferPhase = RESET;

        RewriteData rewriteData = this.player.getRewriteData();
        rewriteData.setTransferCallback(null);

        StopSoundPacket soundPacket = new StopSoundPacket();
        soundPacket.setSoundName("*");
        soundPacket.setStoppingAllSound(true);
        this.player.sendPacketImmediately(soundPacket);
        
        if (!this.connection.isConnected()) {
            this.onTransferFailed();
            return;
        }

        SetLocalPlayerAsInitializedPacket initializedPacket = new SetLocalPlayerAsInitializedPacket();
        initializedPacket.setRuntimeEntityId(this.player.getRewriteData().getOriginalEntityId());
        this.connection.sendPacket(initializedPacket);

        TransferCompleteEvent event = new TransferCompleteEvent(this.sourceServer, this.connection, this.player);
        this.player.getProxy().getEventManager().callEvent(event);
    }

    public void onTransferFailed() {
        if (this.player.sendToFallback(this.targetServer, "Transfer failed")) {
            this.player.sendMessage(new TranslationContainer("waterdog.connected.fallback", this.targetServer.getServerName()));
        } else {
            this.player.disconnect(new TranslationContainer("waterdog.downstream.transfer.failed", targetServer.getServerName(), "Server was closed"));
        }

        this.connection.disconnect();
        this.player.getLogger().warning("Failed to transfer " + this.player.getName() + " to " + this.targetServer.getServerName() + ": Server was closed");
    }

    private int getChunkRadius() {
        int chunkRadius = this.player.getLoginData().getChunkRadius().getRadius();
        if (chunkRadius >= 12) {
            return chunkRadius - 4;
        }
        return chunkRadius - 2;
    }
}
