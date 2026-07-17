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
import dev.waterdog.waterdogpe.network.protocol.handler.downstream.ConnectedDownstreamHandler;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo;
import org.cloudburstmc.protocol.bedrock.packet.DisconnectPacket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
}