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

package dev.waterdog.waterdogpe.network.protocol.handler.upstream;

import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.packs.PackManager;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.ResourcePackChunkDataPacket;
import org.cloudburstmc.protocol.bedrock.packet.ResourcePackChunkRequestPacket;
import org.cloudburstmc.protocol.bedrock.packet.ResourcePackClientResponsePacket;
import org.cloudburstmc.protocol.bedrock.packet.ResourcePackDataInfoPacket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResourcePacksHandlerTest {

    private static final String VERSION = "1.0.0";

    private ProxiedPlayer player;
    private PackManager packManager;
    private ResourcePacksHandler handler;

    @BeforeEach
    void setUp() {
        this.player = mock(ProxiedPlayer.class);
        ProxyServer proxy = mock(ProxyServer.class);
        this.packManager = mock(PackManager.class);
        when(this.player.isConnected()).thenReturn(true);
        when(this.player.getProxy()).thenReturn(proxy);
        when(proxy.getPackManager()).thenReturn(this.packManager);
        when(this.packManager.packChunkDataPacket(anyString(), any())).thenAnswer(inv -> {
            ResourcePackChunkDataPacket data = new ResourcePackChunkDataPacket();
            data.setChunkIndex(inv.<ResourcePackChunkRequestPacket>getArgument(1).getChunkIndex());
            return data;
        });
        this.handler = new ResourcePacksHandler(this.player);
    }

    @Test
    void offersEveryRequestedPackUpFront() {
        ResourcePackDataInfoPacket first = this.pack(3);
        ResourcePackDataInfoPacket second = this.pack(2);

        this.requestPacks(first, second, first);

        verify(this.player, times(1)).sendPacket(first);
        verify(this.player, times(1)).sendPacket(second);
    }

    @Test
    void sendsEachChunkOnce() {
        ResourcePackDataInfoPacket first = this.pack(3);
        this.requestPacks(first);

        this.handler.handle(this.chunk(first, 1));
        this.handler.handle(this.chunk(first, 1));

        assertEquals(List.of(1), this.sentChunkIndexes());
    }

    @Test
    void ignoresChunksOutsideOfferedPacks() {
        ResourcePackDataInfoPacket offered = this.pack(2);
        ResourcePackDataInfoPacket notOffered = this.pack(2);
        this.requestPacks(offered);

        this.handler.handle(this.chunk(offered, -1));
        this.handler.handle(this.chunk(offered, 2));
        this.handler.handle(this.chunk(notOffered, 0));
        this.handler.handle(this.chunk(notOffered, 1));

        verify(this.player, never()).sendPacketImmediately(any());
    }

    private List<Integer> sentChunkIndexes() {
        ArgumentCaptor<BedrockPacket> sent = ArgumentCaptor.forClass(BedrockPacket.class);
        verify(this.player, atLeast(0)).sendPacketImmediately(sent.capture());
        return sent.getAllValues().stream().map(packet -> ((ResourcePackChunkDataPacket) packet).getChunkIndex()).toList();
    }

    private ResourcePackDataInfoPacket pack(int chunkCount) {
        ResourcePackDataInfoPacket info = new ResourcePackDataInfoPacket();
        info.setPackId(UUID.randomUUID());
        info.setPackVersion(VERSION);
        info.setChunkCount(chunkCount);
        when(this.packManager.packInfoFromIdVer(info.getPackId() + "_" + VERSION)).thenReturn(info);
        return info;
    }

    private void requestPacks(ResourcePackDataInfoPacket... packs) {
        ResourcePackClientResponsePacket response = new ResourcePackClientResponsePacket();
        response.setStatus(ResourcePackClientResponsePacket.Status.SEND_PACKS);
        for (ResourcePackDataInfoPacket pack : packs) {
            response.getPackIds().add(pack.getPackId() + "_" + VERSION);
        }
        this.handler.handle(response);
    }

    private ResourcePackChunkRequestPacket chunk(ResourcePackDataInfoPacket pack, int index) {
        ResourcePackChunkRequestPacket request = new ResourcePackChunkRequestPacket();
        request.setPackId(pack.getPackId());
        request.setPackVersion(VERSION);
        request.setChunkIndex(index);
        return request;
    }
}
