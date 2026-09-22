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

package dev.waterdog.waterdogpe.network.protocol;

import dev.waterdog.waterdogpe.network.connection.ProxiedConnection;
import dev.waterdog.waterdogpe.network.protocol.handler.ProxyBatchBridge;
import dev.waterdog.waterdogpe.network.protocol.handler.ProxyPacketHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.cloudburstmc.protocol.bedrock.PacketDirection;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.codec.PacketSerializeException;
import org.cloudburstmc.protocol.bedrock.netty.BedrockBatchWrapper;
import org.cloudburstmc.protocol.bedrock.netty.BedrockPacketWrapper;
import org.cloudburstmc.protocol.bedrock.packet.AddItemEntityPacket;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.MobEquipmentPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetTimePacket;
import org.cloudburstmc.protocol.common.PacketSignal;
import org.cloudburstmc.protocol.common.util.VarInts;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ProxyBatchBridgeTest {

    private static final BedrockCodec CODEC = ProtocolVersion.latest().getDefaultCodec();

    private static int id(Class<? extends BedrockPacket> packet) {
        return CODEC.getPacketDefinition(packet).getId();
    }

    /**
     * Body that decodes the entity ids and then runs out of bytes in the item.
     */
    private static BedrockPacketWrapper truncated(Class<? extends BedrockPacket> packet) {
        ByteBuf buf = Unpooled.buffer();
        VarInts.writeLong(buf, 506);
        VarInts.writeUnsignedLong(buf, 506);
        return BedrockPacketWrapper.create(id(packet), 0, 0, null, buf);
    }

    private static BedrockPacketWrapper setTime() {
        SetTimePacket packet = new SetTimePacket();
        packet.setTime(1000);
        ByteBuf buf = Unpooled.buffer();
        CODEC.tryEncode(CODEC.createHelper(), buf, packet);
        return BedrockPacketWrapper.create(id(SetTimePacket.class), 0, 0, null, buf);
    }

    private static ProxiedConnection source(PacketDirection direction) {
        ProxiedConnection source = mock(ProxiedConnection.class);
        when(source.getPacketDirection()).thenReturn(direction);
        when(source.getSocketAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 19132));
        return source;
    }

    private static ProxyPacketHandler handler() {
        ProxyPacketHandler handler = mock(ProxyPacketHandler.class, RETURNS_DEEP_STUBS);
        when(handler.handlePacket(any())).thenReturn(PacketSignal.UNHANDLED);
        when(handler.doPacketRewrite(any())).thenReturn(PacketSignal.UNHANDLED);
        return handler;
    }

    @Test
    public void undecodableDownstreamPacketIsForwardedUntouched() {
        ProxyPacketHandler handler = handler();
        // A downstream connection sends to the server, so it receives client bound packets
        ProxyBatchBridge bridge = new ProxyBatchBridge(CODEC, CODEC.createHelper(), handler, PacketDirection.SERVER_BOUND);

        BedrockPacketWrapper broken = truncated(AddItemEntityPacket.class);
        ByteBuf brokenBuffer = broken.getPacketBuffer();
        BedrockBatchWrapper batch = BedrockBatchWrapper.newInstance();
        batch.getPackets().add(broken);
        batch.getPackets().add(setTime());

        assertDoesNotThrow(() -> bridge.onBedrockBatch(source(PacketDirection.SERVER_BOUND), batch));

        verify(handler).sendProxiedBatch(batch);
        assertEquals(2, batch.getPackets().size());
        assertNull(broken.getPacket());
        assertSame(brokenBuffer, broken.getPacketBuffer());
        assertFalse(batch.isModified());
        // Only the packet that decoded reached the handler
        verify(handler, times(1)).handlePacket(any());
        assertInstanceOf(SetTimePacket.class, batch.getPackets().get(1).getPacket());
    }

    @Test
    public void undecodableUpstreamPacketStillFails() {
        ProxyBatchBridge bridge = new ProxyBatchBridge(CODEC, CODEC.createHelper(), handler(), PacketDirection.CLIENT_BOUND);

        BedrockBatchWrapper batch = BedrockBatchWrapper.newInstance();
        batch.getPackets().add(truncated(MobEquipmentPacket.class));

        assertThrows(PacketSerializeException.class, () -> bridge.onBedrockBatch(source(PacketDirection.CLIENT_BOUND), batch));
    }
}
