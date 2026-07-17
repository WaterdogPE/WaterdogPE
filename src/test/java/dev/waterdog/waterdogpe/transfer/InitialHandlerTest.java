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
import dev.waterdog.waterdogpe.network.connection.handler.ReconnectReason;
import dev.waterdog.waterdogpe.network.protocol.handler.downstream.InitialHandler;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo;
import org.cloudburstmc.protocol.bedrock.packet.DisconnectPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayStatusPacket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Failure handling during the initial (first join) connection, where no previous
 * downstream exists to stay on.
 */
public class InitialHandlerTest {

    private TransferTestHarness harness;
    private ServerInfo initialServer;
    private ClientConnection initialConnection;
    private InitialHandler handler;

    @BeforeEach
    void setUp() {
        this.harness = new TransferTestHarness();
        this.initialServer = this.harness.newServer("hub");
        this.initialConnection = this.harness.newDownstream(this.initialServer);
        this.harness.setPendingConnection(this.initialConnection);
        this.handler = new InitialHandler(this.harness.player, this.initialConnection);
    }

    @AfterEach
    void tearDown() {
        this.harness.close();
    }

    private PlayStatusPacket loginFailed() {
        PlayStatusPacket packet = new PlayStatusPacket();
        packet.setStatus(PlayStatusPacket.Status.LOGIN_FAILED_SERVER_OLD);
        return packet;
    }

    @Test
    void loginFailureWithoutFallbackKicksWithoutNpe() {
        assertDoesNotThrow(() -> this.handler.handle(loginFailed()));

        assertFalse(this.harness.player.isConnected());
        verify(this.harness.reconnectHandler).getFallbackServer(this.harness.player, this.initialServer,
                ReconnectReason.TRANSFER_FAILED, "Incompatible version");
    }

    @Test
    void loginFailureUsesFallbackWhenAvailable() {
        ServerInfo fallback = this.harness.newServer("fallback");
        this.harness.stubDial(fallback);
        when(this.harness.reconnectHandler.getFallbackServer(any(), any(), any(), anyString())).thenReturn(fallback);

        this.handler.handle(loginFailed());

        verify(fallback).createConnection(this.harness.player);
        assertTrue(this.harness.player.isConnected());
    }

    @Test
    void initialKickCarriesTheKickMessage() {
        DisconnectPacket packet = new DisconnectPacket();
        packet.setKickMessage("You are not whitelisted");

        this.handler.handle(packet);

        assertFalse(this.harness.player.isConnected());
        assertEquals(1, this.harness.events(ServerTransferFailedEvent.class).size());
        ServerTransferFailedEvent event = this.harness.events(ServerTransferFailedEvent.class).get(0);
        assertSame(ReconnectReason.SERVER_KICK, event.getReason());
        assertEquals("You are not whitelisted", event.getMessage());
        assertTrue(this.harness.sentMessages.contains("waterdog.downstream.transfer.failed"),
                "kick reason must reach the player instead of being lost on channel close");
    }
}