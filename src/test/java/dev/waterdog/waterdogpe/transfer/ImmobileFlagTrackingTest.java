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
import dev.waterdog.waterdogpe.network.protocol.ProtocolVersion;
import dev.waterdog.waterdogpe.network.protocol.handler.ProxyBatchBridge;
import dev.waterdog.waterdogpe.network.protocol.handler.downstream.ConnectedDownstreamHandler;
import dev.waterdog.waterdogpe.network.protocol.handler.downstream.SwitchDownstreamHandler;
import dev.waterdog.waterdogpe.network.protocol.rewrite.types.RewriteData;
import org.cloudburstmc.protocol.bedrock.PacketDirection;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.packet.SetEntityDataPacket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImmobileFlagTrackingTest {

    private static final BedrockCodec CODEC = ProtocolVersion.latest().getDefaultCodec();
    private static final long CLIENT_ENTITY_ID = 12345;
    private static final long SERVER_ENTITY_ID = 7;

    private TransferTestHarness harness;
    private RewriteData rewriteData;
    private ProxyBatchBridge lobbyBridge;

    @BeforeEach
    void setUp() {
        this.harness = new TransferTestHarness();
        ClientConnection lobby = this.harness.newDownstream(this.harness.newServer("lobby"));
        this.harness.setActiveDownstream(lobby);
        this.harness.player.setCanRewrite(true);

        this.rewriteData = this.harness.player.getRewriteData();
        this.rewriteData.setEntityId(CLIENT_ENTITY_ID);
        this.rewriteData.setOriginalRuntimeEntityId(SERVER_ENTITY_ID);

        // A downstream bridge receives client bound packets
        this.lobbyBridge = new ProxyBatchBridge(CODEC, CODEC.createHelper(),
                new ConnectedDownstreamHandler(this.harness.player, lobby), PacketDirection.SERVER_BOUND);
    }

    @AfterEach
    void tearDown() {
        this.harness.close();
    }

    private static SetEntityDataPacket immobile(long runtimeId, boolean immobile) {
        SetEntityDataPacket packet = new SetEntityDataPacket();
        packet.setRuntimeEntityId(runtimeId);
        packet.getMetadata().setFlag(EntityFlag.NO_AI, immobile);
        return packet;
    }

    @Test
    void tracksTheImmobileFlagOfThePlayer() {
        this.lobbyBridge.handlePacket(immobile(SERVER_ENTITY_ID, true));
        assertTrue(this.rewriteData.hasImmobileFlag());

        this.lobbyBridge.handlePacket(immobile(SERVER_ENTITY_ID, false));
        assertFalse(this.rewriteData.hasImmobileFlag());
    }

    @Test
    void keepsTheImmobileFlagWhenTheFlagsAreMissing() {
        this.lobbyBridge.handlePacket(immobile(SERVER_ENTITY_ID, true));

        SetEntityDataPacket packet = new SetEntityDataPacket();
        packet.setRuntimeEntityId(SERVER_ENTITY_ID);
        packet.getMetadata().put(EntityDataTypes.SCALE, 2f);
        this.lobbyBridge.handlePacket(packet);

        assertTrue(this.rewriteData.hasImmobileFlag());
    }

    @Test
    void ignoresOtherEntities() {
        // The rewrite swaps an entity holding the client's id over to the player's server id
        this.lobbyBridge.handlePacket(immobile(CLIENT_ENTITY_ID, true));
        this.lobbyBridge.handlePacket(immobile(999, true));

        assertFalse(this.rewriteData.hasImmobileFlag());
    }

    @Test
    void ignoresServersThePlayerIsNotOn() {
        ClientConnection game = this.harness.newDownstream(this.harness.newServer("game"));
        this.harness.setPendingConnection(game);

        new SwitchDownstreamHandler(this.harness.player, game).handle(immobile(SERVER_ENTITY_ID, true));

        assertFalse(this.rewriteData.hasImmobileFlag());
    }

    @Test
    void transferDropsTheOldServersImmobileFlag() {
        this.rewriteData.setImmobileFlag(true);
        ClientConnection game = this.harness.newDownstream(this.harness.newServer("game"));
        this.harness.setPendingConnection(game);

        new SwitchDownstreamHandler(this.harness.player, game).handle(SwitchDownstreamHandlerTest.newStartGame());

        assertFalse(this.rewriteData.hasImmobileFlag());
    }
}
