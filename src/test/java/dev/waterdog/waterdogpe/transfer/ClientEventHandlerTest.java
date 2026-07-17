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

import dev.waterdog.waterdogpe.event.defaults.ServerTransferFailedEvent;
import dev.waterdog.waterdogpe.network.connection.client.ClientConnection;
import dev.waterdog.waterdogpe.network.connection.codec.client.ClientEventHandler;
import dev.waterdog.waterdogpe.network.connection.handler.ReconnectReason;
import dev.waterdog.waterdogpe.network.protocol.handler.TransferCallback;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo;
import io.netty.channel.embedded.EmbeddedChannel;
import org.cloudburstmc.netty.channel.raknet.RakDisconnectReason;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Characterization tests for the downstream failure signals delivered through the netty pipeline:
 * channel exceptions, RakNet timeouts and channel closes must route by the connection's role.
 */
public class ClientEventHandlerTest {

    private TransferTestHarness harness;
    private ServerInfo lobbyServer;
    private ClientConnection lobbyConnection;
    private ServerInfo targetServer;
    private ClientConnection targetConnection;
    private EmbeddedChannel channel;

    @BeforeEach
    void setUp() {
        this.harness = new TransferTestHarness();
        this.lobbyServer = this.harness.newServer("lobby");
        this.lobbyConnection = this.harness.newDownstream(this.lobbyServer);
        this.harness.setActiveDownstream(this.lobbyConnection);
        this.targetServer = this.harness.newServer("game");
        this.targetConnection = this.harness.newDownstream(this.targetServer);
    }

    @AfterEach
    void tearDown() {
        this.harness.close();
    }

    private EmbeddedChannel channelFor(ClientConnection connection) {
        this.channel = new EmbeddedChannel(new ClientEventHandler(this.harness.player, connection));
        return this.channel;
    }

    @Test
    void exceptionOnClaimedConnectionFailsTransfer() {
        TransferCallback callback = new TransferCallback(this.harness.player, this.targetConnection, this.lobbyServer, 0);
        assertTrue(this.harness.player.getRewriteData().trySetTransferCallback(callback));
        this.harness.setActiveDownstream(this.targetConnection);

        channelFor(this.targetConnection).pipeline().fireExceptionCaught(new Exception("boom"));

        assertNull(this.harness.player.getRewriteData().getTransferCallback(), "transfer must be released");
        assertEquals(1, this.harness.events(ServerTransferFailedEvent.class).size());
        assertSame(ReconnectReason.TRANSFER_FAILED, this.harness.events(ServerTransferFailedEvent.class).get(0).getReason());
    }

    @Test
    void exceptionOnPendingConnectionRecovers() {
        this.harness.setPendingConnection(this.targetConnection);

        channelFor(this.targetConnection).pipeline().fireExceptionCaught(new Exception("boom"));

        assertNull(this.harness.player.getPendingConnection());
        assertSame(this.lobbyConnection, this.harness.player.getDownstreamConnection());
        assertTrue(this.harness.player.isConnected());
        assertEquals(1, this.harness.events(ServerTransferFailedEvent.class).size());
        ServerTransferFailedEvent event = this.harness.events(ServerTransferFailedEvent.class).get(0);
        assertSame(ReconnectReason.EXCEPTION, event.getReason());
        assertTrue(event.isRecoverable());
    }

    @Test
    void exceptionOnActiveConnectionFallsBack() {
        ServerInfo fallback = this.harness.newServer("fallback");
        this.harness.stubDial(fallback);
        when(this.harness.reconnectHandler.getFallbackServer(any(), any(), any(), anyString())).thenReturn(fallback);

        channelFor(this.lobbyConnection).pipeline().fireExceptionCaught(new Exception("boom"));

        verify(this.lobbyConnection).disconnect();
        verify(fallback).createConnection(this.harness.player);
        assertTrue(this.harness.player.isConnected());
        assertTrue(this.harness.sentMessages.contains("waterdog.downstream.down"));
    }

    @Test
    void exceptionOnActiveConnectionKicksWithoutFallback() {
        channelFor(this.lobbyConnection).pipeline().fireExceptionCaught(new Exception("boom"));

        assertFalse(this.harness.player.isConnected(), "no fallback leaves nothing but a kick");
    }

    @Test
    void exceptionIgnoredWhenChannelInactive() {
        EmbeddedChannel embeddedChannel = channelFor(this.targetConnection);
        embeddedChannel.close();
        this.harness.events.clear();

        embeddedChannel.pipeline().fireExceptionCaught(new Exception("boom"));

        assertTrue(this.harness.events(ServerTransferFailedEvent.class).isEmpty());
        assertTrue(this.harness.player.isConnected());
    }

    @Test
    void rakTimeoutRoutesPendingRecovery() {
        this.harness.setPendingConnection(this.targetConnection);

        channelFor(this.targetConnection).pipeline().fireUserEventTriggered(RakDisconnectReason.TIMED_OUT);

        assertNull(this.harness.player.getPendingConnection());
        assertTrue(this.harness.player.isConnected());
        assertEquals(1, this.harness.events(ServerTransferFailedEvent.class).size());
        assertSame(ReconnectReason.TIMEOUT, this.harness.events(ServerTransferFailedEvent.class).get(0).getReason());
    }

    @Test
    void channelCloseRoutesPendingRecovery() {
        this.harness.setPendingConnection(this.targetConnection);

        channelFor(this.targetConnection).close();

        assertNull(this.harness.player.getPendingConnection());
        assertTrue(this.harness.player.isConnected());
        assertEquals(1, this.harness.events(ServerTransferFailedEvent.class).size());
        assertSame(ReconnectReason.UNKNOWN, this.harness.events(ServerTransferFailedEvent.class).get(0).getReason());
    }
}