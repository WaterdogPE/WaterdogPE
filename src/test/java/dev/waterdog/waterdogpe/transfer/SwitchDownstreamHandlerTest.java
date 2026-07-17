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

import dev.waterdog.waterdogpe.event.defaults.ServerTransferEvent;
import dev.waterdog.waterdogpe.event.defaults.ServerTransferFailedEvent;
import dev.waterdog.waterdogpe.network.connection.client.ClientConnection;
import dev.waterdog.waterdogpe.network.connection.handler.ReconnectReason;
import dev.waterdog.waterdogpe.network.protocol.handler.TransferCallback;
import dev.waterdog.waterdogpe.network.protocol.handler.downstream.SwitchDownstreamHandler;
import dev.waterdog.waterdogpe.network.protocol.rewrite.types.StartGameSettings;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.AuthoritativeMovementMode;
import org.cloudburstmc.protocol.bedrock.data.GameType;
import org.cloudburstmc.protocol.bedrock.packet.DisconnectPacket;
import org.cloudburstmc.protocol.bedrock.packet.StartGamePacket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class SwitchDownstreamHandlerTest {

    private TransferTestHarness harness;
    private ServerInfoFixture lobby;
    private ServerInfoFixture target;
    private SwitchDownstreamHandler handler;

    private record ServerInfoFixture(dev.waterdog.waterdogpe.network.serverinfo.ServerInfo info, ClientConnection connection) {
    }

    private ServerInfoFixture newFixture(String name) {
        var info = this.harness.newServer(name);
        return new ServerInfoFixture(info, this.harness.newDownstream(info));
    }

    @BeforeEach
    void setUp() {
        this.harness = new TransferTestHarness();
        this.lobby = newFixture("lobby");
        this.target = newFixture("game");
        this.harness.setActiveDownstream(this.lobby.connection());
        this.harness.setPendingConnection(this.target.connection());
        this.harness.player.getRewriteData().setStartGameSettings(StartGameSettings.from(newStartGame()));
        this.handler = new SwitchDownstreamHandler(this.harness.player, this.target.connection());
    }

    @AfterEach
    void tearDown() {
        this.harness.close();
    }

    private static StartGamePacket newStartGame() {
        StartGamePacket packet = new StartGamePacket();
        packet.setRuntimeEntityId(100);
        packet.setUniqueEntityId(100);
        packet.setPlayerPosition(Vector3f.from(0, 64, 0));
        packet.setRotation(Vector2f.ZERO);
        packet.setDimensionId(0);
        packet.setPlayerGameType(GameType.SURVIVAL);
        packet.setLevelGameType(GameType.SURVIVAL);
        packet.setDifficulty(1);
        packet.setDayCycleStopTime(0);
        packet.setAuthoritativeMovementMode(AuthoritativeMovementMode.SERVER);
        packet.setRewindHistorySize(20);
        packet.setServerAuthoritativeBlockBreaking(true);
        packet.setInventoriesServerAuthoritative(true);
        packet.setBlockNetworkIdsHashed(false);
        return packet;
    }

    @Test
    void abortsStartGameFromDiscardedConnection() {
        this.harness.setPendingConnection(null);

        this.handler.handle(newStartGame());

        verify(this.target.connection()).disconnect();
        assertNull(this.harness.player.getRewriteData().getTransferCallback());
        assertSame(this.lobby.connection(), this.harness.player.getDownstreamConnection());
    }

    @Test
    void abortsStartGameWhileAnotherTransferIsActive() {
        ServerInfoFixture other = newFixture("other");
        TransferCallback active = new TransferCallback(this.harness.player, other.connection(), this.lobby.info(), 0);
        assertTrue(this.harness.player.getRewriteData().trySetTransferCallback(active));

        this.handler.handle(newStartGame());

        verify(this.target.connection()).disconnect();
        assertSame(active, this.harness.player.getRewriteData().getTransferCallback(), "the active transfer must keep the slot");
        assertTrue(this.harness.sentMessages.contains("waterdog.downstream.connecting"));
        assertSame(this.lobby.connection(), this.harness.player.getDownstreamConnection());
    }

    @Test
    void failsTransferOnIncompatibleStartGameSettings() {
        StartGamePacket packet = newStartGame();
        packet.setAuthoritativeMovementMode(AuthoritativeMovementMode.CLIENT);
        packet.setRewindHistorySize(0);

        this.handler.handle(packet);

        verify(this.target.connection()).disconnect();
        assertNull(this.harness.player.getRewriteData().getTransferCallback(), "incompatible transfer must not claim the slot");
        assertSame(this.lobby.connection(), this.harness.player.getDownstreamConnection());
        assertNull(this.harness.player.getPendingConnection());
        assertTrue(this.harness.player.isConnected());

        assertEquals(1, this.harness.events(ServerTransferFailedEvent.class).size());
        ServerTransferFailedEvent event = this.harness.events(ServerTransferFailedEvent.class).get(0);
        assertSame(ReconnectReason.INCOMPATIBLE, event.getReason());
        assertTrue(event.isRecoverable());
    }

    @Test
    void claimsTransferAndActivatesQueue() {
        this.handler.handle(newStartGame());

        TransferCallback callback = this.harness.player.getRewriteData().getTransferCallback();
        assertNotNull(callback, "transfer slot should be claimed");
        assertEquals(TransferCallback.TransferPhase.PHASE_1, callback.getPhase());
        assertSame(this.target.connection(), callback.getConnection());

        assertSame(this.target.connection(), this.harness.player.getDownstreamConnection());
        assertNull(this.harness.player.getPendingConnection());
        verify(this.lobby.connection()).setPacketHandler(null);
        verify(this.lobby.connection()).disconnect();
        verify(this.harness.upstream).setTransferQueueActive(true);

        assertEquals(1, this.harness.events(ServerTransferEvent.class).size());
        assertEquals(1, this.harness.scheduledTasks.size(), "transfer timeout should be armed");
        assertEquals(60 * 20, this.harness.scheduledTasks.get(0).delayTicks());
    }

    @Test
    void kickAfterClaimFailsTheTransfer() {
        this.handler.handle(newStartGame());
        assertNotNull(this.harness.player.getRewriteData().getTransferCallback());

        DisconnectPacket kick = new DisconnectPacket();
        kick.setKickMessage("kicked");
        this.handler.handle(kick);

        assertNull(this.harness.player.getRewriteData().getTransferCallback());
        verify(this.harness.upstream).discardTransferQueue();
        assertEquals(1, this.harness.events(ServerTransferFailedEvent.class).size());
        assertSame(ReconnectReason.TRANSFER_FAILED, this.harness.events(ServerTransferFailedEvent.class).get(0).getReason());
    }

    @Test
    void kickBeforeStartGameIsRecoverable() {
        DisconnectPacket kick = new DisconnectPacket();
        kick.setKickMessage("kicked");
        this.handler.handle(kick);

        verify(this.target.connection()).disconnect();
        assertSame(this.lobby.connection(), this.harness.player.getDownstreamConnection());
        assertTrue(this.harness.player.isConnected());

        assertEquals(1, this.harness.events(ServerTransferFailedEvent.class).size());
        ServerTransferFailedEvent event = this.harness.events(ServerTransferFailedEvent.class).get(0);
        assertSame(ReconnectReason.SERVER_KICK, event.getReason());
        assertTrue(event.isRecoverable());
    }
}