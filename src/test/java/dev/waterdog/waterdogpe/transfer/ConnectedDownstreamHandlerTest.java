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

import dev.waterdog.waterdogpe.network.connection.client.ClientConnection;
import dev.waterdog.waterdogpe.network.protocol.handler.TransferCallback;
import dev.waterdog.waterdogpe.network.protocol.handler.downstream.ConnectedDownstreamHandler;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.packet.DisconnectPacket;
import org.cloudburstmc.protocol.bedrock.packet.RespawnPacket;
import dev.waterdog.waterdogpe.network.protocol.Signals;
import org.cloudburstmc.protocol.common.PacketSignal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentCaptor.forClass;
import org.mockito.ArgumentCaptor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Characterization tests for kicks from the active downstream server.
 */
public class ConnectedDownstreamHandlerTest {

    private TransferTestHarness harness;
    private ServerInfo lobbyServer;
    private ClientConnection lobbyConnection;
    private ConnectedDownstreamHandler handler;

    @BeforeEach
    void setUp() {
        this.harness = new TransferTestHarness();
        this.lobbyServer = this.harness.newServer("lobby");
        this.lobbyConnection = this.harness.newDownstream(this.lobbyServer);
        this.harness.setActiveDownstream(this.lobbyConnection);
        this.handler = new ConnectedDownstreamHandler(this.harness.player, this.lobbyConnection);
    }

    @AfterEach
    void tearDown() {
        this.harness.close();
    }

    private DisconnectPacket kick(String message) {
        DisconnectPacket packet = new DisconnectPacket();
        packet.setKickMessage(message);
        return packet;
    }

    @Test
    void kickWithFallbackTransfersSilently() {
        ServerInfo fallback = this.harness.newServer("fallback");
        this.harness.stubDial(fallback);
        when(this.harness.reconnectHandler.getFallbackServer(any(), any(), any(), anyString())).thenReturn(fallback);

        this.handler.handle(kick("bye"));

        verify(fallback).createConnection(this.harness.player);
        assertTrue(this.harness.player.isConnected());
        assertFalse(this.harness.sentMessages.contains("bye"), "fallback kick shows no message");
    }

    @Test
    void kickWithoutFallbackDisconnectsWithKickMessage() {
        this.handler.handle(kick("bye"));

        assertFalse(this.harness.player.isConnected());
        assertTrue(this.harness.sentMessages.contains("waterdog.downstream.kicked"), "kick reason must reach the player");
    }

    private static RespawnPacket respawn(RespawnPacket.State state) {
        RespawnPacket packet = new RespawnPacket();
        packet.setRuntimeEntityId(42);
        packet.setPosition(Vector3f.from(8, 64, 8));
        packet.setState(state);
        return packet;
    }

    /**
     * A server the player is being transferred to starts its spawn sequence with a respawn
     * handshake. The client never asked to respawn and will not answer it, so the proxy answers on
     * its behalf - otherwise the server never starts ticking the player and nothing they do counts.
     */
    @Test
    void answersTheRespawnHandshakeOfATransferInFlight() {
        TransferCallback callback = new TransferCallback(this.harness.player, this.lobbyConnection,
                this.harness.newServer("game"), 0);
        assertTrue(this.harness.player.getRewriteData().trySetTransferCallback(callback));

        assertSame(Signals.CANCEL, this.handler.handle(respawn(RespawnPacket.State.SERVER_SEARCHING)));

        ArgumentCaptor<RespawnPacket> response = forClass(RespawnPacket.class);
        verify(this.lobbyConnection).sendPacket(response.capture());
        assertEquals(RespawnPacket.State.CLIENT_READY, response.getValue().getState());
        assertEquals(42, response.getValue().getRuntimeEntityId());
    }

    /** A respawn on the server the player already plays on is a real death: leave it to the client. */
    @Test
    void leavesARealRespawnToTheClient() {
        assertSame(PacketSignal.UNHANDLED, this.handler.handle(respawn(RespawnPacket.State.SERVER_SEARCHING)));
        verify(this.lobbyConnection, never()).sendPacket(isA(RespawnPacket.class));
    }
}
