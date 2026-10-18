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
import dev.waterdog.waterdogpe.network.nethernet.NetherNetClientConnection;
import dev.waterdog.waterdogpe.network.protocol.user.LoginData;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo;
import org.cloudburstmc.protocol.bedrock.data.auth.AuthType;
import org.cloudburstmc.protocol.bedrock.data.auth.TokenPayload;
import org.cloudburstmc.protocol.bedrock.packet.LoginPacket;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OriginalLoginForwardingTest {

    @Test
    void preservesClientCredentialsWithoutSharingThePacket() {
        LoginPacket original = new LoginPacket();
        original.setProtocolVersion(2193);
        original.setClientJwt("client-signed-jwt");
        original.setAuthPayload(new TokenPayload("xbox-signed-token", AuthType.FULL));

        LoginData data = LoginData.builder().xboxAuthed(true).originalLoginPacket(original).build();
        LoginPacket forwarded = data.getOriginalLoginPacket();
        assertNotSame(original, forwarded);
        assertEquals(original.getProtocolVersion(), forwarded.getProtocolVersion());
        assertEquals(original.getClientJwt(), forwarded.getClientJwt());
        assertSame(original.getAuthPayload(), forwarded.getAuthPayload());
    }

    @Test
    void refusesUnauthenticatedOriginalLogin() {
        LoginData data = LoginData.builder().xboxAuthed(false).originalLoginPacket(new LoginPacket()).build();
        assertThrows(IllegalStateException.class, data::getOriginalLoginPacket);
    }

    @Test
    void forwardsOnlyToNamedNetherNetBackendInOnlineMode() {
        try (TransferTestHarness harness = new TransferTestHarness()) {
            LoginPacket original = new LoginPacket();
            LoginPacket proxySigned = new LoginPacket();
            when(harness.config.getForwardOriginalLoginTo()).thenReturn(List.of("greatworld"));
            when(harness.config.isOnlineMode()).thenReturn(true);
            when(harness.loginData.getOriginalLoginPacket()).thenReturn(original);
            when(harness.loginData.getLoginPacket()).thenReturn(proxySigned);

            NetherNetClientConnection nethernet = mock(NetherNetClientConnection.class);
            ServerInfo greatworld = mock(ServerInfo.class);
            when(greatworld.getServerName()).thenReturn("greatworld");
            when(nethernet.getServerInfo()).thenReturn(greatworld);
            assertSame(original, harness.player.getDownstreamLoginPacket(nethernet));

            ClientConnection other = mock(ClientConnection.class);
            ServerInfo elsewhere = mock(ServerInfo.class);
            when(elsewhere.getServerName()).thenReturn("elsewhere");
            when(other.getServerInfo()).thenReturn(elsewhere);
            assertSame(proxySigned, harness.player.getDownstreamLoginPacket(other));

            when(other.getServerInfo()).thenReturn(greatworld);
            assertThrows(IllegalStateException.class, () -> harness.player.getDownstreamLoginPacket(other));
            when(harness.config.isOnlineMode()).thenReturn(false);
            assertThrows(IllegalStateException.class, () -> harness.player.getDownstreamLoginPacket(nethernet));
        }
    }
}
