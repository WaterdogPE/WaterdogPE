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

import dev.waterdog.waterdogpe.event.EventManager;
import dev.waterdog.waterdogpe.event.EventPriority;
import dev.waterdog.waterdogpe.event.defaults.ServerTransferFailedEvent;
import dev.waterdog.waterdogpe.event.defaults.ServerTransferRequestEvent;
import dev.waterdog.waterdogpe.network.connection.client.ClientConnection;
import dev.waterdog.waterdogpe.network.connection.handler.ReconnectReason;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Plugin code is not trusted: throwing reconnect handlers, event handlers and broken
 * ServerInfo implementations must never break failure handling or leak attempt state.
 */
public class PluginFaultToleranceTest {

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

    @Test
    void throwingTransferFailureHandlerIsTreatedAsStay() {
        when(this.harness.reconnectHandler.getTransferFailureServer(any(), any(), any(), anyString()))
                .thenThrow(new RuntimeException("plugin bug"));
        ServerInfo target = this.harness.newServer("game");
        ClientConnection connection = this.harness.newDownstream(target);
        this.harness.setPendingConnection(connection);

        assertDoesNotThrow(() -> this.harness.player.onTransferFailure(connection, target, ReconnectReason.SERVER_KICK, "kicked"));

        assertTrue(this.harness.player.isConnected());
        assertSame(this.lobbyConnection, this.harness.player.getDownstreamConnection());
        assertTrue(this.harness.sentMessages.contains("waterdog.downstream.transfer.failed"));
    }

    @Test
    void throwingFallbackHandlerIsTreatedAsNoFallback() {
        when(this.harness.reconnectHandler.getFallbackServer(any(), any(), any(), anyString()))
                .thenThrow(new RuntimeException("plugin bug"));

        assertDoesNotThrow(() -> this.harness.player.onDownstreamDisconnected(this.lobbyConnection));

        assertFalse(this.harness.player.isConnected(), "no usable fallback leaves nothing but a kick");
    }

    @Test
    void throwingServerInfoDialLeavesNoResidue() {
        ServerInfo target = this.harness.newServer("game");
        when(target.createConnection(any())).thenThrow(new IllegalStateException("broken impl"));

        assertDoesNotThrow(() -> this.harness.player.connect(target));

        assertTrue(this.harness.player.isConnected(), "player stays on the previous server");
        assertTrue(this.harness.player.getPendingServers().isEmpty(), "a leaked pendingServers entry would block the target forever");
        assertEquals(1, this.harness.events(ServerTransferFailedEvent.class).size());

        // The target must be dialable again, not stuck on "already connecting".
        this.harness.stubDial(target);
        this.harness.player.connect(target);
        verify(target, times(2)).createConnection(this.harness.player);
    }

    @Test
    void throwingEventHandlerDoesNotBreakDispatchOrOtherHandlers() {
        EventManager eventManager = new EventManager(this.harness.proxy);
        AtomicBoolean otherHandlerRan = new AtomicBoolean(false);
        eventManager.subscribe(ServerTransferRequestEvent.class, event -> {
            throw new RuntimeException("plugin bug");
        }, EventPriority.NORMAL);
        eventManager.subscribe(ServerTransferRequestEvent.class, event -> otherHandlerRan.set(true), EventPriority.NORMAL);

        ServerTransferRequestEvent event = new ServerTransferRequestEvent(this.harness.player, this.lobbyServer);
        assertNull(assertDoesNotThrow(() -> eventManager.callEvent(event)), "sync non-completable events return no future");
        assertTrue(otherHandlerRan.get(), "handlers after the broken one must still run");
        assertFalse(event.isCancelled());
    }
}