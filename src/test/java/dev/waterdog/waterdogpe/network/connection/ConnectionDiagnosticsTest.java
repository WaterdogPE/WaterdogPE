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

package dev.waterdog.waterdogpe.network.connection;

import dev.waterdog.waterdogpe.network.connection.client.BedrockClientConnection;
import dev.waterdog.waterdogpe.network.connection.codec.compression.ProxiedCompressionCodec;
import dev.waterdog.waterdogpe.network.protocol.ProtocolVersion;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfoType;
import io.netty.channel.embedded.EmbeddedChannel;
import org.cloudburstmc.protocol.bedrock.data.PacketCompressionAlgorithm;
import org.cloudburstmc.protocol.bedrock.netty.codec.compression.CompressionCodec;
import org.cloudburstmc.protocol.bedrock.netty.codec.compression.SimpleCompressionStrategy;
import org.cloudburstmc.protocol.bedrock.netty.codec.compression.ZlibCompression;
import org.cloudburstmc.protocol.bedrock.netty.codec.encryption.BedrockEncryptionEncoder;
import org.cloudburstmc.protocol.common.util.Zlib;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConnectionDiagnosticsTest {

    private static final ProtocolVersion PROTOCOL = ProtocolVersion.latest();

    private static Map<String, ConnectionDiagnostics.Line> report(EmbeddedChannel channel, boolean supportsEncryption) {
        return report(channel, supportsEncryption, true);
    }

    private static Map<String, ConnectionDiagnostics.Line> report(EmbeddedChannel channel, boolean supportsEncryption, boolean addresses) {
        ServerInfo server = mock(ServerInfo.class);
        when(server.getServerName()).thenReturn("lobby");
        when(server.getServerType()).thenReturn(ServerInfoType.BEDROCK);
        when(server.getAddress()).thenReturn(new InetSocketAddress("10.0.0.2", 19132));

        BedrockClientConnection connection = mock(BedrockClientConnection.class);
        when(connection.getServerInfo()).thenReturn(server);
        when(connection.getChannel()).thenReturn(channel);
        when(connection.getSocketAddress()).thenReturn(new InetSocketAddress("10.0.0.2", 19132));
        when(connection.supportsEncryption()).thenReturn(supportsEncryption);

        List<ConnectionDiagnostics.Line> lines = ConnectionDiagnostics.downstream(connection, PROTOCOL, addresses);
        return lines.stream().collect(Collectors.toMap(ConnectionDiagnostics.Line::label, line -> line));
    }

    @Test
    void reportsNegotiatedCodecsFromThePipeline() throws Exception {
        EmbeddedChannel channel = new EmbeddedChannel();
        ZlibCompression zlib = new ZlibCompression(Zlib.RAW);
        zlib.setLevel(7);
        channel.pipeline().addLast(CompressionCodec.NAME, new ProxiedCompressionCodec(new SimpleCompressionStrategy(zlib), true));
        SecretKeySpec key = new SecretKeySpec(new byte[32], "AES");
        Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new javax.crypto.spec.IvParameterSpec(new byte[16]));
        channel.pipeline().addLast(BedrockEncryptionEncoder.NAME, new BedrockEncryptionEncoder(key, cipher));

        Map<String, ConnectionDiagnostics.Line> lines = report(channel, true);

        assertEquals("lobby (bedrock)", lines.get("Server").value());
        assertEquals("AES-CTR", lines.get("Encryption").value());
        assertEquals(PacketCompressionAlgorithm.ZLIB + " level 7", lines.get("Compression").value());
        assertEquals("RakNet protocol " + PROTOCOL.getRaknetVersion() + " rules", lines.get("Framing").value());
        // An embedded channel is neither transport, so the report says what it saw
        assertEquals("EmbeddedChannel", lines.get("Transport").value());
    }

    @Test
    void hidesAddressesFromPlayers() {
        Map<String, ConnectionDiagnostics.Line> full = report(new EmbeddedChannel(), true, true);
        Map<String, ConnectionDiagnostics.Line> hidden = report(new EmbeddedChannel(), true, false);

        assertEquals("/10.0.0.2:19132", full.get("Configured").value());
        assertNotNull(full.get("Address"));
        assertNull(hidden.get("Configured"));
        assertNull(hidden.get("Address"));
        assertEquals("lobby (bedrock)", hidden.get("Server").value());
    }

    @Test
    void explainsMissingEncryptionOnNetherNet() {
        Map<String, ConnectionDiagnostics.Line> lines = report(new EmbeddedChannel(), false);

        assertEquals("none", lines.get("Encryption").value());
        assertNotNull(lines.get("Encryption").note());
        assertEquals("not negotiated yet", lines.get("Compression").value());
        assertNull(lines.get("Compression").note());
        assertEquals("RakNet protocol 11 rules", lines.get("Framing").value());
    }
}
