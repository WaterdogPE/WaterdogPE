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

import dev.waterdog.waterdogpe.event.defaults.ServerPreConnectEvent;
import dev.waterdog.waterdogpe.event.defaults.ServerTransferFailedEvent;
import dev.waterdog.waterdogpe.network.connection.client.ClientConnection;
import dev.waterdog.waterdogpe.network.connection.handler.ReconnectReason;
import dev.waterdog.waterdogpe.network.protocol.handler.TransferCallback;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo;
import io.netty.util.concurrent.Promise;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProxiedPlayerTransferFailureTest {

    private TransferTestHarness harness;
    private ServerInfo lobbyServer;
    private ClientConnection lobbyConnection;

    @BeforeEach
    void setUp() {
        this.harness = new TransferTestHarness();
        this.lobbyServer = this.harness.newServer("lobby");
        this.lobbyConnection = this.harness.newDownstream(this.lobbyServer);
        this.harness.setActiveDownstream(this.lobbyConnection);
    }

    @AfterEach
    void tearDown() {
        this.harness.close();
    }

    private TransferCallback claimTransfer(ClientConnection connection) {
        TransferCallback callback = new TransferCallback(this.harness.player, connection,
                this.lobbyServer, 0);
        assertTrue(this.harness.player.getRewriteData().trySetTransferCallback(callback));
        return callback;
    }

    @Test
    void deniesConnectWhileTransferInProgress() {
        ClientConnection targetConnection = this.harness.newDownstream(this.harness.newServer("game"));
        claimTransfer(targetConnection);

        ServerInfo other = this.harness.newServer("other");
        this.harness.player.connect(other);

        verify(other, never()).createConnection(any());
        assertTrue(this.harness.sentMessages.contains("waterdog.downstream.connecting"));
    }

    @Test
    void cancelledPreConnectEventAbortsTransfer() {
        this.harness.eventInterceptor = event -> {
            if (event instanceof ServerPreConnectEvent preConnectEvent) {
                preConnectEvent.setCancelled(true);
            }
        };
        ServerInfo target = this.harness.newServer("game");
        this.harness.stubDial(target);

        this.harness.player.connect(target);

        verify(target, never()).createConnection(any());
        assertTrue(this.harness.player.getPendingServers().isEmpty(), "cancelled transfer must not leave the target pending");
    }

    @Test
    void pendingFailureStaysOnCurrentServerByDefault() {
        ServerInfo target = this.harness.newServer("game");
        ClientConnection connection = this.harness.newDownstream(target);
        this.harness.setPendingConnection(connection);

        this.harness.player.onTransferFailure(connection, target, ReconnectReason.SERVER_KICK, "kicked");

        verify(connection).disconnect();
        assertNull(this.harness.player.getPendingConnection());
        assertSame(this.lobbyConnection, this.harness.player.getDownstreamConnection());
        assertTrue(this.harness.player.isConnected());
        verify(this.harness.reconnectHandler).getTransferFailureServer(this.harness.player, target, ReconnectReason.SERVER_KICK, "kicked");
        assertTrue(this.harness.sentMessages.contains("waterdog.downstream.transfer.failed"));

        assertEquals(1, this.harness.events(ServerTransferFailedEvent.class).size());
        ServerTransferFailedEvent event = this.harness.events(ServerTransferFailedEvent.class).get(0);
        assertSame(target, event.getTargetServer());
        assertSame(ReconnectReason.SERVER_KICK, event.getReason());
        assertTrue(event.isRecoverable());
    }

    @Test
    void pendingFailureReconnectsWhenHandlerPicksServer() {
        ServerInfo target = this.harness.newServer("game");
        ClientConnection connection = this.harness.newDownstream(target);
        this.harness.setPendingConnection(connection);

        ServerInfo backup = this.harness.newServer("backup");
        this.harness.stubDial(backup);
        when(this.harness.reconnectHandler.getTransferFailureServer(any(), any(), any(), anyString())).thenReturn(backup);

        this.harness.player.onTransferFailure(connection, target, ReconnectReason.SERVER_KICK, "kicked");

        verify(backup).createConnection(this.harness.player);
        assertTrue(this.harness.player.isConnected());
    }

    @Test
    void discardedConnectionDoesNotTriggerRecovery() {
        ServerInfo target = this.harness.newServer("game");
        ClientConnection discarded = this.harness.newDownstream(target);

        this.harness.player.onTransferFailure(discarded, target, ReconnectReason.SERVER_KICK, "kicked");

        verify(discarded).disconnect();
        verify(this.harness.reconnectHandler, never()).getTransferFailureServer(any(), any(), any(), anyString());
        assertTrue(this.harness.events(ServerTransferFailedEvent.class).isEmpty());
    }

    @Test
    void claimedConnectionIsLeftToTransferFailurePaths() {
        ServerInfo target = this.harness.newServer("game");
        ClientConnection connection = this.harness.newDownstream(target);
        this.harness.setPendingConnection(connection);
        claimTransfer(connection);

        this.harness.player.onTransferFailure(connection, target, ReconnectReason.TIMEOUT, "Transfer timed out");

        verify(connection, never()).disconnect();
        assertSame(connection, this.harness.player.getPendingConnection());
        assertTrue(this.harness.events(ServerTransferFailedEvent.class).isEmpty());
    }

    @Test
    void dialFailureDoesNotStompNewerAttempt() {
        ServerInfo newerTarget = this.harness.newServer("newer");
        ClientConnection newerConnection = this.harness.newDownstream(newerTarget);
        this.harness.setPendingConnection(newerConnection);

        ServerInfo failedTarget = this.harness.newServer("game");
        this.harness.player.onTransferFailure(null, failedTarget, ReconnectReason.EXCEPTION, "boom");

        assertSame(newerConnection, this.harness.player.getPendingConnection(), "the in-flight attempt must survive");
        verify(this.harness.reconnectHandler, never()).getTransferFailureServer(any(), any(), any(), anyString());
        assertEquals(1, this.harness.events(ServerTransferFailedEvent.class).size());
        assertTrue(this.harness.sentMessages.contains("waterdog.downstream.transfer.failed"));
    }

    @Test
    void pendingConnectionDeathTriggersRecovery() {
        ServerInfo target = this.harness.newServer("game");
        ClientConnection connection = this.harness.newDownstream(target);
        this.harness.setPendingConnection(connection);

        this.harness.player.onDownstreamDisconnected(connection);

        assertNull(this.harness.player.getPendingConnection());
        assertTrue(this.harness.player.isConnected());
        assertEquals(1, this.harness.events(ServerTransferFailedEvent.class).size());
        assertTrue(this.harness.events(ServerTransferFailedEvent.class).get(0).isRecoverable());
    }

    @Test
    void midTransferTargetDeathFailsTransfer() {
        ServerInfo target = this.harness.newServer("game");
        ClientConnection connection = this.harness.newDownstream(target);
        TransferCallback callback = claimTransfer(connection);
        this.harness.setActiveDownstream(connection); // START_GAME already hijacked the player

        this.harness.player.onDownstreamDisconnected(connection);

        assertEquals(TransferCallback.TransferPhase.RESET, callback.getPhase());
        assertNull(this.harness.player.getRewriteData().getTransferCallback());
        verify(this.harness.upstream).discardTransferQueue();
        assertEquals(1, this.harness.events(ServerTransferFailedEvent.class).size());
        assertSame(ReconnectReason.TRANSFER_FAILED, this.harness.events(ServerTransferFailedEvent.class).get(0).getReason());
    }

    @Test
    void activeDownstreamDeathFallsBack() {
        ServerInfo fallback = this.harness.newServer("fallback");
        this.harness.stubDial(fallback);
        when(this.harness.reconnectHandler.getFallbackServer(any(), any(), any(), anyString())).thenReturn(fallback);

        this.harness.player.onDownstreamDisconnected(this.lobbyConnection);

        verify(fallback).createConnection(this.harness.player);
        assertTrue(this.harness.player.isConnected());
    }

    @Test
    void pendingTimeoutTriggersRecovery() {
        ServerInfo target = this.harness.newServer("game");
        ClientConnection connection = this.harness.newDownstream(target);
        this.harness.setPendingConnection(connection);

        this.harness.player.onDownstreamTimeout(target);

        verify(connection).disconnect();
        assertNull(this.harness.player.getPendingConnection());
        assertEquals(1, this.harness.events(ServerTransferFailedEvent.class).size());
        assertSame(ReconnectReason.TIMEOUT, this.harness.events(ServerTransferFailedEvent.class).get(0).getReason());
        assertTrue(this.harness.events(ServerTransferFailedEvent.class).get(0).isRecoverable());
    }

    @Test
    void midTransferTimeoutFailsTransfer() {
        ServerInfo target = this.harness.newServer("game");
        ClientConnection connection = this.harness.newDownstream(target);
        claimTransfer(connection);
        this.harness.setActiveDownstream(connection);

        this.harness.player.onDownstreamTimeout(target);

        assertNull(this.harness.player.getRewriteData().getTransferCallback());
        assertEquals(1, this.harness.events(ServerTransferFailedEvent.class).size());
        assertSame(ReconnectReason.TRANSFER_FAILED, this.harness.events(ServerTransferFailedEvent.class).get(0).getReason());
    }

    @Test
    void watchdogRecoversStuckPendingConnection() {
        ServerInfo target = this.harness.newServer("game");
        Promise<ClientConnection> dial = this.harness.stubDial(target);

        this.harness.player.connect(target);
        verify(target).createConnection(this.harness.player);

        ClientConnection connection = this.harness.newDownstream(target);
        dial.setSuccess(connection);
        assertSame(connection, this.harness.player.getPendingConnection());

        assertEquals(1, this.harness.scheduledTasks.size(), "pending watchdog should be armed");
        assertEquals(60 * 20, this.harness.scheduledTasks.get(0).delayTicks());
        this.harness.runScheduledTasks();

        assertNull(this.harness.player.getPendingConnection());
        verify(connection).disconnect();
        assertSame(this.lobbyConnection, this.harness.player.getDownstreamConnection());
        assertEquals(1, this.harness.events(ServerTransferFailedEvent.class).size());
        assertSame(ReconnectReason.TIMEOUT, this.harness.events(ServerTransferFailedEvent.class).get(0).getReason());
    }

    @Test
    void watchdogIgnoresProgressedConnection() {
        ServerInfo target = this.harness.newServer("game");
        Promise<ClientConnection> dial = this.harness.stubDial(target);

        this.harness.player.connect(target);
        ClientConnection connection = this.harness.newDownstream(target);
        dial.setSuccess(connection);

        // START_GAME arrived: the connection is promoted and the pending slot cleared.
        this.harness.player.setDownstreamConnection(connection);
        this.harness.runScheduledTasks();

        verify(connection, never()).disconnect();
        assertTrue(this.harness.events(ServerTransferFailedEvent.class).isEmpty());
    }
}