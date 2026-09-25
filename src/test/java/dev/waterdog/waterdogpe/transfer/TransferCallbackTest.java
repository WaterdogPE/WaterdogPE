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

package dev.waterdog.waterdogpe.transfer;

import dev.waterdog.waterdogpe.event.defaults.PostTransferCompleteEvent;
import dev.waterdog.waterdogpe.event.defaults.ServerTransferFailedEvent;
import dev.waterdog.waterdogpe.network.connection.client.ClientConnection;
import dev.waterdog.waterdogpe.network.connection.handler.ReconnectReason;
import dev.waterdog.waterdogpe.network.protocol.handler.TransferCallback;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.netty.BedrockBatchWrapper;
import org.cloudburstmc.protocol.bedrock.netty.BedrockPacketWrapper;
import org.cloudburstmc.protocol.bedrock.packet.LevelChunkPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetLocalPlayerAsInitializedPacket;
import org.cloudburstmc.protocol.bedrock.packet.UpdateClientInputLocksPacket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TransferCallbackTest {

    private TransferTestHarness harness;
    private ServerInfo sourceServer;
    private ServerInfo targetServer;
    private ClientConnection targetConnection;
    private TransferCallback callback;

    @BeforeEach
    void setUp() {
        this.harness = new TransferTestHarness();
        this.sourceServer = this.harness.newServer("lobby");
        this.targetServer = this.harness.newServer("game");
        this.targetConnection = this.harness.newDownstream(this.targetServer);
        this.callback = new TransferCallback(this.harness.player, this.targetConnection, this.sourceServer, 0);
        assertTrue(this.harness.player.getRewriteData().trySetTransferCallback(this.callback));
    }

    @AfterEach
    void tearDown() {
        this.harness.close();
    }

    private void setPhase(TransferCallback.TransferPhase phase) {
        TransferTestHarness.setField(this.callback, "transferPhase", phase);
    }

    @Test
    void failureReleasesStateAndFiresEventOnce() {
        this.callback.onTransferFailed("test failure");

        assertEquals(TransferCallback.TransferPhase.RESET, this.callback.getPhase());
        assertNull(this.harness.player.getRewriteData().getTransferCallback());
        verify(this.harness.upstream).discardTransferQueue();

        assertEquals(1, this.harness.events(ServerTransferFailedEvent.class).size());
        ServerTransferFailedEvent event = this.harness.events(ServerTransferFailedEvent.class).get(0);
        assertSame(this.targetServer, event.getTargetServer());
        assertSame(ReconnectReason.TRANSFER_FAILED, event.getReason());
        assertFalse(event.isRecoverable());

        // Failing again must be a no-op.
        this.callback.onTransferFailed("test failure");
        assertEquals(1, this.harness.events(ServerTransferFailedEvent.class).size());
    }

    @Test
    void failureKicksPlayerWhenNoFallbackExists() {
        this.callback.onTransferFailed("test failure");
        assertFalse(this.harness.player.isConnected(), "player should be kicked without a fallback");
    }

    @Test
    void failureSendsPlayerToFallback() {
        ServerInfo fallback = this.harness.newServer("fallback");
        this.harness.stubDial(fallback);
        when(this.harness.reconnectHandler.getFallbackServer(any(), any(), any(), anyString())).thenReturn(fallback);

        this.callback.onTransferFailed("test failure");

        assertTrue(this.harness.player.isConnected());
        verify(fallback).createConnection(this.harness.player);
    }

    @Test
    void finalizeRequiresBothResetPhaseAndPlayStatus() {
        setPhase(TransferCallback.TransferPhase.PHASE_2);
        this.callback.onPlayStatus();
        verify(this.targetConnection, never()).sendPacket(isA(SetLocalPlayerAsInitializedPacket.class));

        setPhase(TransferCallback.TransferPhase.RESET);
        this.callback.tryTransferFinalize();

        verify(this.targetConnection).sendPacket(isA(SetLocalPlayerAsInitializedPacket.class));
        assertEquals(1, this.harness.events(PostTransferCompleteEvent.class).size());
        assertNull(this.harness.player.getRewriteData().getTransferCallback(), "callback should be released on finalize");
    }

    @Test
    void finalizeHappensOnlyOnce() {
        setPhase(TransferCallback.TransferPhase.RESET);
        this.callback.onPlayStatus();
        this.callback.tryTransferFinalize();
        this.callback.onPlayStatus();

        verify(this.targetConnection).sendPacket(isA(SetLocalPlayerAsInitializedPacket.class));
        assertEquals(1, this.harness.events(PostTransferCompleteEvent.class).size());
    }

    @Test
    void lateSpawnAfterFailureDoesNotFinalize() {
        ServerInfo fallback = this.harness.newServer("fallback");
        this.harness.stubDial(fallback);
        when(this.harness.reconnectHandler.getFallbackServer(any(), any(), any(), anyString())).thenReturn(fallback);

        this.callback.onTransferFailed("test failure");
        this.callback.onPlayStatus();

        verify(this.targetConnection, never()).sendPacket(isA(SetLocalPlayerAsInitializedPacket.class));
        assertTrue(this.harness.events(PostTransferCompleteEvent.class).isEmpty());
    }

    @Test
    void phaseOneInputLockUsesSpawnPosition() {
        Vector3f spawnPosition = Vector3f.from(128, 64, -32);
        this.harness.player.getRewriteData().setSpawnPosition(spawnPosition);

        this.callback.onDimChangeSuccess();

        ArgumentCaptor<UpdateClientInputLocksPacket> packet = ArgumentCaptor.forClass(UpdateClientInputLocksPacket.class);
        verify(this.harness.upstream).sendPacket(packet.capture());
        assertEquals(spawnPosition, packet.getValue().getServerPosition());
    }

    /**
     * The chunks the proxy injects for the second dimension change have to stand on their own. Once
     * the transfer reaches phase 2 the client's sub-chunk requests are forwarded to the new server,
     * which never streamed these columns, so a request mode chunk leaves the client waiting on a
     * dimension change that never completes - the transfer then times out in phase 2 with the world
     * still empty. The mode of the server the player is leaving must not leak into them.
     */
    @Test
    void injectedChunksNeverAskTheNewServerForSubChunks() {
        this.harness.player.getRewriteData().setSpawnPosition(Vector3f.from(0, 64, 0));
        this.harness.player.getRewriteData().setRotation(Vector2f.ZERO);
        this.harness.player.setSubChunkRequestMode(true); // left a server that streams sub-chunks
        this.harness.player.getRewriteData().setDimension(1); // forces the second dimension change

        this.callback.onDimChangeSuccess();

        ArgumentCaptor<BedrockBatchWrapper> batches = ArgumentCaptor.forClass(BedrockBatchWrapper.class);
        verify(this.harness.upstream, atLeastOnce()).sendPacket(batches.capture());

        List<LevelChunkPacket> chunks = batches.getAllValues().stream()
                .flatMap(batch -> batch.getPackets().stream())
                .map(BedrockPacketWrapper::getPacket)
                .filter(LevelChunkPacket.class::isInstance)
                .map(LevelChunkPacket.class::cast)
                .toList();

        assertFalse(chunks.isEmpty(), "the dimension change must carry the chunks it needs");
        for (LevelChunkPacket chunk : chunks) {
            assertFalse(chunk.isRequestSubChunks(), "injected chunks must not be request mode");
        }
    }

    @Test
    void timeoutKicksStuckTransfer() {
        this.callback.startTimeout();
        assertEquals(1, this.harness.scheduledTasks.size());
        assertEquals(60 * 20, this.harness.scheduledTasks.get(0).delayTicks());

        this.harness.runScheduledTasks();

        assertFalse(this.harness.player.isConnected(), "stuck transfer should kick the player");
        assertNull(this.harness.player.getRewriteData().getTransferCallback());
        assertEquals(1, this.harness.events(ServerTransferFailedEvent.class).size());
        ServerTransferFailedEvent event = this.harness.events(ServerTransferFailedEvent.class).get(0);
        assertSame(ReconnectReason.TIMEOUT, event.getReason());
        assertFalse(event.isRecoverable());
    }

    @Test
    void timeoutIsCancelledAndIgnoredAfterFinalize() {
        this.callback.startTimeout();
        TransferTestHarness.Scheduled scheduled = this.harness.scheduledTasks.get(0);

        setPhase(TransferCallback.TransferPhase.RESET);
        this.callback.onPlayStatus();
        verify(scheduled.handler()).cancel();

        this.harness.runScheduledTasks();
        assertTrue(this.harness.player.isConnected(), "finalized transfer must not kick the player");
        assertTrue(this.harness.events(ServerTransferFailedEvent.class).isEmpty());
    }

    @Test
    void timeoutAfterFailureDoesNothing() {
        this.callback.startTimeout();
        ServerInfo fallback = this.harness.newServer("fallback");
        this.harness.stubDial(fallback);
        when(this.harness.reconnectHandler.getFallbackServer(any(), any(), any(), anyString())).thenReturn(fallback);

        this.callback.onTransferFailed("test failure");
        this.harness.runScheduledTasks();

        assertTrue(this.harness.player.isConnected());
        assertEquals(1, this.harness.events(ServerTransferFailedEvent.class).size(), "only the failure event should exist");
        assertSame(ReconnectReason.TRANSFER_FAILED, this.harness.events(ServerTransferFailedEvent.class).get(0).getReason());
    }
}
