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
import dev.waterdog.waterdogpe.network.protocol.handler.downstream.CompressionInitHandler;
import dev.waterdog.waterdogpe.network.protocol.handler.downstream.InitialHandler;
import dev.waterdog.waterdogpe.network.protocol.handler.downstream.SwitchDownstreamHandler;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo;
import org.cloudburstmc.protocol.bedrock.data.DisconnectFailReason;
import org.cloudburstmc.protocol.bedrock.packet.DisconnectPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayStatusPacket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Refusals that arrive before NetworkSettings, which is where a server turns away an incompatible
 * protocol.
 */
public class CompressionInitHandlerTest {

    private TransferTestHarness harness;
    private ServerInfo server;
    private ClientConnection connection;

    @BeforeEach
    void setUp() {
        this.harness = new TransferTestHarness();
        this.server = this.harness.newServer("hub");
        this.connection = this.harness.newDownstream(this.server);
    }

    @AfterEach
    void tearDown() {
        this.harness.close();
    }

    private static PlayStatusPacket incompatible() {
        PlayStatusPacket packet = new PlayStatusPacket();
        packet.setStatus(PlayStatusPacket.Status.LOGIN_FAILED_CLIENT_OLD);
        return packet;
    }

    private CompressionInitHandler joining() {
        this.harness.setPendingConnection(this.connection);
        return new CompressionInitHandler(this.harness.player, this.connection,
                new InitialHandler(this.harness.player, this.connection));
    }

    @Test
    void incompatibleVersionFailsTheJoinAndIsLogged() {
        this.joining().handle(incompatible());

        verify(this.harness.reconnectHandler).getFallbackServer(this.harness.player, this.server,
                ReconnectReason.TRANSFER_FAILED, "Incompatible version");
        verify(this.harness.logger).info(anyString(), any(), eq("TestPlayer"), eq("hub"),
                eq("Incompatible version (LOGIN_FAILED_CLIENT_OLD)"));
        assertFalse(this.harness.player.isConnected());
    }

    @Test
    void kickBeforeNetworkSettingsKeepsItsMessage() {
        DisconnectPacket packet = new DisconnectPacket();
        packet.setKickMessage("disconnectionScreen.outdatedServer");

        this.joining().handle(packet);

        ServerTransferFailedEvent event = this.harness.events(ServerTransferFailedEvent.class).get(0);
        assertSame(ReconnectReason.SERVER_KICK, event.getReason());
        assertEquals("disconnectionScreen.outdatedServer", event.getMessage());
    }

    @Test
    void theDisconnectAfterARefusalIsNotLoggedAgain() {
        CompressionInitHandler handler = this.joining();
        DisconnectPacket disconnect = new DisconnectPacket();
        disconnect.setKickMessage("");

        handler.handle(incompatible());
        handler.handle(disconnect);

        verify(this.harness.logger, times(1)).info(eq("[{}|{}] -> Downstream [{}] refused the connection: {}"),
                any(), any(), any(), any());
    }

    @Test
    void anEmptyKickMessageLogsTheReason() {
        DisconnectPacket packet = new DisconnectPacket();
        packet.setKickMessage("");
        packet.setReason(DisconnectFailReason.OUTDATED_SERVER);

        this.joining().handle(packet);

        verify(this.harness.logger).info(anyString(), any(), eq("TestPlayer"), eq("hub"), eq("OUTDATED_SERVER"));
    }

    @Test
    void incompatibleVersionFailsTheTransfer() {
        ClientConnection lobby = this.harness.newDownstream(this.harness.newServer("lobby"));
        this.harness.setActiveDownstream(lobby);
        this.harness.setPendingConnection(this.connection);
        CompressionInitHandler handler = new CompressionInitHandler(this.harness.player, this.connection,
                new SwitchDownstreamHandler(this.harness.player, this.connection));

        handler.handle(incompatible());

        ServerTransferFailedEvent event = this.harness.events(ServerTransferFailedEvent.class).get(0);
        assertEquals("Incompatible version", event.getMessage());
        assertTrue(this.harness.player.isConnected(), "the player stays on the lobby");
    }
}
