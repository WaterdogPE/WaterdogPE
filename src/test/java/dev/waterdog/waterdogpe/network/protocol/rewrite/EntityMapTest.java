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

package dev.waterdog.waterdogpe.network.protocol.rewrite;

import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.network.protocol.rewrite.types.RewriteData;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import dev.waterdog.waterdogpe.transfer.TransferTestHarness;
import dev.waterdog.waterdogpe.utils.config.proxy.ProxyConfig;
import org.cloudburstmc.protocol.bedrock.packet.ShowCreditsPacket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EntityMapTest {

    private static final long PROXY_ENTITY_ID = 12345;
    private static final long DOWNSTREAM_ENTITY_ID = 7;

    private EntityMap entityMap;

    @BeforeEach
    void setUp() {
        // RewriteData reads the proxy name on construction
        ProxyServer proxy = mock(ProxyServer.class);
        ProxyConfig config = mock(ProxyConfig.class);
        when(proxy.getConfiguration()).thenReturn(config);
        when(config.getName()).thenReturn("TestProxy");
        TransferTestHarness.setProxyInstance(proxy);

        RewriteData rewriteData = new RewriteData();
        rewriteData.setEntityId(PROXY_ENTITY_ID);
        rewriteData.setOriginalEntityId(DOWNSTREAM_ENTITY_ID);

        ProxiedPlayer player = mock(ProxiedPlayer.class);
        when(player.getRewriteData()).thenReturn(rewriteData);
        when(player.canRewrite()).thenReturn(true);

        this.entityMap = new EntityMap(player);
    }

    @AfterEach
    void tearDown() {
        TransferTestHarness.setProxyInstance(null);
    }

    @Test
    void rewritesTheCreditsPacketOnTheWayToTheClient() {
        ShowCreditsPacket packet = new ShowCreditsPacket();
        packet.setRuntimeEntityId(DOWNSTREAM_ENTITY_ID);
        packet.setStatus(ShowCreditsPacket.Status.START_CREDITS);

        this.entityMap.doRewrite(packet);

        assertEquals(PROXY_ENTITY_ID, packet.getRuntimeEntityId());
    }

    @Test
    void rewritesTheCreditsPacketOnTheWayToTheServer() {
        ShowCreditsPacket packet = new ShowCreditsPacket();
        packet.setRuntimeEntityId(PROXY_ENTITY_ID);
        packet.setStatus(ShowCreditsPacket.Status.END_CREDITS);

        this.entityMap.doRewrite(packet);

        assertEquals(DOWNSTREAM_ENTITY_ID, packet.getRuntimeEntityId());
    }

    @Test
    void leavesOtherEntitiesAlone() {
        ShowCreditsPacket packet = new ShowCreditsPacket();
        packet.setRuntimeEntityId(999);
        packet.setStatus(ShowCreditsPacket.Status.START_CREDITS);

        this.entityMap.doRewrite(packet);

        assertEquals(999, packet.getRuntimeEntityId());
    }
}
