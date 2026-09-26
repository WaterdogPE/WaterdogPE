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
import dev.waterdog.waterdogpe.network.protocol.Signals;
import dev.waterdog.waterdogpe.network.protocol.handler.TransferCallback;
import dev.waterdog.waterdogpe.network.protocol.handler.downstream.ConnectedDownstreamHandler;
import dev.waterdog.waterdogpe.network.protocol.handler.downstream.SwitchDownstreamHandler;
import dev.waterdog.waterdogpe.network.protocol.handler.upstream.ConnectedUpstreamHandler;
import dev.waterdog.waterdogpe.network.protocol.rewrite.types.RewriteData;
import dev.waterdog.waterdogpe.network.protocol.rewrite.types.StartGameSettings;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.AttributeData;
import org.cloudburstmc.protocol.bedrock.data.PlayerActionType;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityEventType;
import org.cloudburstmc.protocol.bedrock.packet.EntityEventPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerActionPacket;
import org.cloudburstmc.protocol.bedrock.packet.RespawnPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetHealthPacket;
import org.cloudburstmc.protocol.bedrock.packet.UpdateAttributesPacket;
import org.cloudburstmc.protocol.common.PacketSignal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class DeathScreenTransferTest {

    private static final long CLIENT_ENTITY_ID = 12345;
    private static final long SERVER_ENTITY_ID = 7;

    private TransferTestHarness harness;
    private RewriteData rewriteData;
    private ClientConnection lobby;
    private ClientConnection target;

    @BeforeEach
    void setUp() {
        this.harness = new TransferTestHarness();
        this.lobby = this.harness.newDownstream(this.harness.newServer("lobby"));
        this.target = this.harness.newDownstream(this.harness.newServer("game"));
        this.harness.setActiveDownstream(this.lobby);
        this.harness.setPendingConnection(this.target);

        this.rewriteData = this.harness.player.getRewriteData();
        this.rewriteData.setEntityId(CLIENT_ENTITY_ID);
        this.rewriteData.setOriginalEntityId(SERVER_ENTITY_ID);
        this.rewriteData.setStartGameSettings(StartGameSettings.from(SwitchDownstreamHandlerTest.newStartGame()));
    }

    @AfterEach
    void tearDown() {
        this.harness.close();
    }

    private static UpdateAttributesPacket health(long runtimeId, float value) {
        UpdateAttributesPacket packet = new UpdateAttributesPacket();
        packet.setRuntimeEntityId(runtimeId);
        packet.getAttributes().add(new AttributeData("minecraft:health", 0, 20, value));
        return packet;
    }

    private static RespawnPacket respawn(RespawnPacket.State state) {
        RespawnPacket packet = new RespawnPacket();
        packet.setState(state);
        packet.setPosition(Vector3f.ZERO);
        return packet;
    }

    private static PlayerActionPacket respawnAction() {
        PlayerActionPacket packet = new PlayerActionPacket();
        packet.setAction(PlayerActionType.RESPAWN);
        return packet;
    }

    @Test
    void tracksDeathFromHealthOfThePlayer() {
        ConnectedDownstreamHandler handler = new ConnectedDownstreamHandler(this.harness.player, this.lobby);

        handler.handle(health(SERVER_ENTITY_ID, 0));
        assertTrue(this.rewriteData.isDead());

        handler.handle(health(SERVER_ENTITY_ID, 20));
        assertFalse(this.rewriteData.isDead());
    }

    @Test
    void tracksDeathFromTheLegacyHealthPacket() {
        ConnectedDownstreamHandler handler = new ConnectedDownstreamHandler(this.harness.player, this.lobby);
        SetHealthPacket packet = new SetHealthPacket();

        packet.setHealth(0);
        handler.handle(packet);
        assertTrue(this.rewriteData.isDead());

        packet.setHealth(20);
        handler.handle(packet);
        assertFalse(this.rewriteData.isDead());
    }

    @Test
    void tracksDeathFromTheRespawnHandshake() {
        ConnectedDownstreamHandler handler = new ConnectedDownstreamHandler(this.harness.player, this.lobby);

        handler.handle(respawn(RespawnPacket.State.SERVER_SEARCHING));
        assertTrue(this.rewriteData.isDead());

        handler.handle(respawn(RespawnPacket.State.SERVER_READY));
        assertFalse(this.rewriteData.isDead());
    }

    @Test
    void ignoresOtherEntitiesAndInactiveServers() {
        new ConnectedDownstreamHandler(this.harness.player, this.lobby).handle(health(999, 0));
        new ConnectedDownstreamHandler(this.harness.player, this.target).handle(health(SERVER_ENTITY_ID, 0));

        assertFalse(this.rewriteData.isDead());
    }

    @Test
    void transferWhileDeadEndsTheDeathScreen() {
        this.rewriteData.setDead(true);

        new SwitchDownstreamHandler(this.harness.player, this.target).handle(SwitchDownstreamHandlerTest.newStartGame());

        verify(this.harness.upstream).sendPacketImmediately(argThat(packet -> packet instanceof RespawnPacket respawn
                && respawn.getState() == RespawnPacket.State.SERVER_READY
                && respawn.getRuntimeEntityId() == CLIENT_ENTITY_ID));
        assertFalse(this.rewriteData.isDead());
        assertTrue(this.rewriteData.isProxyRespawn());
    }

    @Test
    void transferWhileAliveLeavesTheRespawnAlone() {
        new SwitchDownstreamHandler(this.harness.player, this.target).handle(SwitchDownstreamHandlerTest.newStartGame());

        verify(this.harness.upstream, never()).sendPacketImmediately(argThat(packet -> packet instanceof RespawnPacket));
        assertFalse(this.rewriteData.isProxyRespawn());
    }

    @Test
    void finishesTheRespawnTheProxyStarted() {
        this.rewriteData.setProxyRespawn(true);
        ConnectedUpstreamHandler handler = new ConnectedUpstreamHandler(this.harness.player);

        assertEquals(Signals.CANCEL, handler.handle(respawn(RespawnPacket.State.CLIENT_READY)));
        assertEquals(Signals.CANCEL, handler.handle(respawnAction()));

        verify(this.harness.upstream).sendPacketImmediately(argThat(packet -> packet instanceof EntityEventPacket event
                && event.getType() == EntityEventType.RESPAWN
                && event.getRuntimeEntityId() == CLIENT_ENTITY_ID));
        assertFalse(this.rewriteData.isProxyRespawn());
    }

    @Test
    void forwardsRespawnsTheProxyDidNotStart() {
        ConnectedUpstreamHandler handler = new ConnectedUpstreamHandler(this.harness.player);

        assertEquals(PacketSignal.UNHANDLED, handler.handle(respawn(RespawnPacket.State.CLIENT_READY)));
        assertEquals(PacketSignal.UNHANDLED, handler.handle(respawnAction()));
        verify(this.harness.upstream, never()).sendPacketImmediately(argThat(packet -> packet instanceof EntityEventPacket));
    }

    @Test
    void newDeathOutdatesAnUnansweredProxyRespawn() {
        ConnectedDownstreamHandler handler = new ConnectedDownstreamHandler(this.harness.player, this.lobby);
        this.rewriteData.setProxyRespawn(true);

        // The new server's own join handshake must not cancel it mid transfer
        TransferCallback transfer = new TransferCallback(this.harness.player, this.lobby, this.harness.newServer("old"), 0);
        this.rewriteData.setTransferCallback(transfer);
        handler.handle(respawn(RespawnPacket.State.SERVER_SEARCHING));
        assertTrue(this.rewriteData.isProxyRespawn());

        this.rewriteData.clearTransferCallback(transfer);
        handler.handle(health(SERVER_ENTITY_ID, 0));
        assertFalse(this.rewriteData.isProxyRespawn());
    }
}
