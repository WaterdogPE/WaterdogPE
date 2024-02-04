/*
 * Copyright 2022 WaterdogTEAM
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

package dev.waterdog.waterdogpe.network.connection.client;

import dev.waterdog.waterdogpe.network.connection.codec.batch.FrameIdCodec;
import dev.waterdog.waterdogpe.network.connection.codec.compression.CompressionType;
import dev.waterdog.waterdogpe.network.connection.codec.compression.ProxiedCompressionCodec;
import dev.waterdog.waterdogpe.network.connection.codec.initializer.ProxiedSessionInitializer;
import dev.waterdog.waterdogpe.network.connection.codec.packet.BedrockPacketCodec;
import dev.waterdog.waterdogpe.network.protocol.ProtocolVersion;
import dev.waterdog.waterdogpe.network.protocol.handler.ProxyBatchBridge;
import dev.waterdog.waterdogpe.network.protocol.handler.ProxyPacketHandler;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.netty.channel.raknet.RakChannel;
import org.cloudburstmc.netty.handler.codec.raknet.common.RakSessionCodec;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodecHelper;
import org.cloudburstmc.protocol.bedrock.codec.v428.Bedrock_v428;
import org.cloudburstmc.protocol.bedrock.data.CompressionAlgorithm;
import org.cloudburstmc.protocol.bedrock.netty.BedrockBatchWrapper;
import org.cloudburstmc.protocol.bedrock.netty.BedrockPacketWrapper;
import org.cloudburstmc.protocol.bedrock.netty.codec.compression.CompressionCodec;
import org.cloudburstmc.protocol.bedrock.netty.codec.compression.CompressionStrategy;
import org.cloudburstmc.protocol.bedrock.netty.codec.encryption.BedrockEncryptionDecoder;
import org.cloudburstmc.protocol.bedrock.netty.codec.encryption.BedrockEncryptionEncoder;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacketHandler;
import org.cloudburstmc.protocol.bedrock.util.EncryptionUtils;

import javax.crypto.SecretKey;
import java.net.SocketAddress;
import java.util.List;
import java.util.Objects;

import static dev.waterdog.waterdogpe.network.connection.codec.initializer.ProxiedSessionInitializer.getCompressionStrategy;

@Log4j2
public class BedrockClientConnection extends SimpleChannelInboundHandler<BedrockBatchWrapper> implements ClientConnection {
    private final ProxiedPlayer player;
    private final ServerInfo serverInfo;
    private final Channel channel;

    private final List<Runnable> disconnectListeners = new ObjectArrayList<>();

    private BedrockPacketHandler packetHandler;
    private CompressionStrategy compressionStrategy;

    public BedrockClientConnection(ProxiedPlayer player, ServerInfo serverInfo, Channel channel) {
        this.player = player;
        this.serverInfo = serverInfo;
        this.channel = channel;
        if (player.getProtocol().isBefore(ProtocolVersion.MINECRAFT_PE_1_19_30)) {
            this.compressionStrategy = player.getConnection().getPeer().getRakVersion() < 10 ?
                    ProxiedSessionInitializer.ZLIB_STRATEGY : ProxiedSessionInitializer.ZLIB_RAW_STRATEGY;
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        this.disconnectListeners.forEach(Runnable::run);
        super.channelInactive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, BedrockBatchWrapper batch) {
        if (this.packetHandler instanceof ProxyBatchBridge bridge) {
            bridge.onBedrockBatch(this, batch);
        } else if (this.packetHandler != null) {
            for (BedrockPacketWrapper packet : batch.getPackets()) {
                this.packetHandler.handlePacket(packet.getPacket());
            }
        } else {
            log.warn("Received unhandled packets for " + this.getSocketAddress());
        }
    }

    @Override
    public void sendPacket(BedrockBatchWrapper wrapper) {
        if (this.player.getProtocol().isBefore(ProtocolVersion.MINECRAFT_PE_1_20_60) &&
                !Objects.equals(wrapper.getAlgorithm(), this.compressionStrategy.getDefaultCompression().getAlgorithm())) {
            wrapper.setCompressed(null); // Before 1.20.60 dynamic compression is not supported
        }
        // Starting with 1.20.60 support all compression algorithms on server side.
        this.channel.writeAndFlush(wrapper);
    }

    @Override
    public void sendPacket(BedrockPacket packet) {
        this.channel.writeAndFlush(packet);
    }

    @Override
    public void sendPacketImmediately(BedrockPacket packet) {
        this.channel.writeAndFlush(BedrockBatchWrapper.create(this.getSubClientId(), packet));
    }

    @Override
    public void setCompression(CompressionAlgorithm algorithm) {
        CompressionStrategy strategy;
        if (algorithm instanceof CompressionType type && type.getBedrockAlgorithm() != null) {
            strategy = getCompressionStrategy(type.getBedrockAlgorithm(), this.player.getProtocol().getRaknetVersion(), false);
        } else {
            strategy = getCompressionStrategy(algorithm, this.player.getProtocol().getRaknetVersion(), false);
        }
        this.setCompressionStrategy(strategy);
    }

    @Override
    public void setCompressionStrategy(CompressionStrategy strategy) {
        boolean needsPrefix = this.player.getProtocol().isAfterOrEqual(ProtocolVersion.MINECRAFT_PE_1_20_60);

        ChannelHandler handler = this.channel.pipeline().get(CompressionCodec.NAME);
        if (handler == null) {
            this.channel.pipeline().addAfter(FrameIdCodec.NAME, CompressionCodec.NAME, new ProxiedCompressionCodec(strategy, needsPrefix));
        } else {
            this.channel.pipeline().replace(CompressionCodec.NAME, CompressionCodec.NAME, new ProxiedCompressionCodec(strategy, needsPrefix));
        }
        this.compressionStrategy = strategy;
    }

    @Override
    public void enableEncryption(SecretKey secretKey) {
        if (!secretKey.getAlgorithm().equals("AES")) {
            throw new IllegalArgumentException("Invalid key algorithm");
        }
        // Check if the codecs exist in the pipeline
        if (this.channel.pipeline().get(BedrockEncryptionEncoder.class) != null ||
                this.channel.pipeline().get(BedrockEncryptionDecoder.class) != null) {
            throw new IllegalStateException("Encryption is already enabled");
        }

        int protocolVersion = this.getCodec().getProtocolVersion();
        boolean useCtr = protocolVersion >= Bedrock_v428.CODEC.getProtocolVersion();

        this.channel.pipeline().addAfter(FrameIdCodec.NAME, BedrockEncryptionEncoder.NAME,
                new BedrockEncryptionEncoder(secretKey, EncryptionUtils.createCipher(useCtr, true, secretKey)));
        this.channel.pipeline().addAfter(FrameIdCodec.NAME, BedrockEncryptionDecoder.NAME,
                new BedrockEncryptionDecoder(secretKey, EncryptionUtils.createCipher(useCtr, false, secretKey)));

        log.info("Encryption enabled for {}", this.getSocketAddress());
    }

    @Override
    public void setCodecHelper(BedrockCodec codec, BedrockCodecHelper helper) {
        Objects.requireNonNull(codec, "codec");
        Objects.requireNonNull(helper, "helper");
        this.channel.pipeline().get(BedrockPacketCodec.class).setCodecHelper(codec, helper);
    }

    private BedrockCodec getCodec() {
        return this.channel.pipeline().get(BedrockPacketCodec.class).getCodec();
    }

    private BedrockCodecHelper getCodecHelper() {
        return this.channel.pipeline().get(BedrockPacketCodec.class).getHelper();
    }

    @Override
    public void disconnect() {
        this.channel.disconnect();
    }

    @Override
    public long getPing() {
        if (this.channel instanceof RakChannel rakChannel) {
            return rakChannel.rakPipeline().get(RakSessionCodec.class).getPing();
        }
        return 0;
    }

    @Override
    public ProxiedPlayer getPlayer() {
        return this.player;
    }

    @Override
    public ServerInfo getServerInfo() {
        return this.serverInfo;
    }

    @Override
    public boolean isConnected() {
        return this.channel.isOpen();
    }

    @Override
    public SocketAddress getSocketAddress() {
        return this.channel.remoteAddress();
    }

    @Override
    public BedrockPacketHandler getPacketHandler() {
        return this.packetHandler;
    }

    @Override
    public void setPacketHandler(BedrockPacketHandler handler) {
        if (handler instanceof ProxyPacketHandler packetHandler) {
            if (this.getPacketHandler() instanceof ProxyBatchBridge bridge) {
                bridge.setHandler(packetHandler);
            } else {
                this.packetHandler = new ProxyBatchBridge(this.getCodec(), this.getCodecHelper(), packetHandler);
            }
        } else {
            this.packetHandler = handler;
        }
    }

    @Override
    public void addDisconnectListener(Runnable listener) {
        this.disconnectListeners.add(listener);
    }

    @Override
    public String toString() {
        return "BedrockClientConnection(player=" + this.player.getName() + ", serverInfo=" + this.serverInfo + ", address=" + this.channel.remoteAddress() + ")";
    }
}
