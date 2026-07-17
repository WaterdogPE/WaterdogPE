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
import org.cloudburstmc.protocol.bedrock.packet.SetLocalPlayerAsInitializedPacket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
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
        this.callback.onTransferFailed();

        assertEquals(TransferCallback.TransferPhase.RESET, this.callback.getPhase());
        assertNull(this.harness.player.getRewriteData().getTransferCallback());
        verify(this.harness.upstream).discardTransferQueue();

        assertEquals(1, this.harness.events(ServerTransferFailedEvent.class).size());
        ServerTransferFailedEvent event = this.harness.events(ServerTransferFailedEvent.class).get(0);
        assertSame(this.targetServer, event.getTargetServer());
        assertSame(ReconnectReason.TRANSFER_FAILED, event.getReason());
        assertFalse(event.isRecoverable());

        // Failing again must be a no-op.
        this.callback.onTransferFailed();
        assertEquals(1, this.harness.events(ServerTransferFailedEvent.class).size());
    }

    @Test
    void failureKicksPlayerWhenNoFallbackExists() {
        this.callback.onTransferFailed();
        assertFalse(this.harness.player.isConnected(), "player should be kicked without a fallback");
    }

    @Test
    void failureSendsPlayerToFallback() {
        ServerInfo fallback = this.harness.newServer("fallback");
        this.harness.stubDial(fallback);
        when(this.harness.reconnectHandler.getFallbackServer(any(), any(), any(), anyString())).thenReturn(fallback);

        this.callback.onTransferFailed();

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

        this.callback.onTransferFailed();
        this.callback.onPlayStatus();

        verify(this.targetConnection, never()).sendPacket(isA(SetLocalPlayerAsInitializedPacket.class));
        assertTrue(this.harness.events(PostTransferCompleteEvent.class).isEmpty());
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

        this.callback.onTransferFailed();
        this.harness.runScheduledTasks();

        assertTrue(this.harness.player.isConnected());
        assertEquals(1, this.harness.events(ServerTransferFailedEvent.class).size(), "only the failure event should exist");
        assertSame(ReconnectReason.TRANSFER_FAILED, this.harness.events(ServerTransferFailedEvent.class).get(0).getReason());
    }
}