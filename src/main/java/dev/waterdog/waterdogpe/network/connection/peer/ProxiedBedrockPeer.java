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

package dev.waterdog.waterdogpe.network.connection.peer;

import dev.waterdog.waterdogpe.network.connection.codec.BedrockBatchWrapper;
import dev.waterdog.waterdogpe.network.connection.codec.batch.FrameIdCodec;
import dev.waterdog.waterdogpe.network.connection.codec.compression.CompressionAlgorithm;
import dev.waterdog.waterdogpe.network.connection.codec.compression.NoopCompressionCodec;
import dev.waterdog.waterdogpe.network.connection.codec.encryption.BedrockEncryptionDecoder;
import dev.waterdog.waterdogpe.network.connection.codec.encryption.BedrockEncryptionEncoder;
import dev.waterdog.waterdogpe.network.connection.codec.initializer.ProxiedSessionInitializer;
import dev.waterdog.waterdogpe.network.connection.codec.packet.BedrockPacketCodec;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.netty.channel.raknet.RakChannel;
import org.cloudburstmc.netty.channel.raknet.config.RakChannelOption;
import org.cloudburstmc.netty.handler.codec.raknet.common.RakSessionCodec;
import org.cloudburstmc.protocol.bedrock.BedrockPeer;
import org.cloudburstmc.protocol.bedrock.BedrockSession;
import org.cloudburstmc.protocol.bedrock.BedrockSessionFactory;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodecHelper;
import org.cloudburstmc.protocol.bedrock.codec.v428.Bedrock_v428;
import org.cloudburstmc.protocol.bedrock.data.PacketCompressionAlgorithm;
import org.cloudburstmc.protocol.bedrock.netty.BedrockPacketWrapper;
import org.cloudburstmc.protocol.bedrock.netty.codec.compression.CompressionCodec;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.util.EncryptionUtils;

import javax.crypto.SecretKey;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Log4j2
public class ProxiedBedrockPeer extends BedrockPeer {
    private BedrockServerSession firstSession;
    private CompressionAlgorithm compressionAlgorithm;

    public ProxiedBedrockPeer(Channel channel, BedrockSessionFactory factory) {
        super(channel, factory);
    }

    private void onBedrockBatch(BedrockBatchWrapper batch) {
        if (this.firstSession == null) {
            for (BedrockPacketWrapper wrapper : batch.getPackets()) {
                this.getSession(wrapper.getTargetSubClientId()).onPacket(wrapper);
            }
        } else {
            this.firstSession.onBedrockBatch(batch);
        }
    }

    private BedrockServerSession getSession(int sessionId) {
        BedrockServerSession session = (BedrockServerSession) this.sessions.computeIfAbsent(sessionId, this::onSessionCreated);
        if (this.firstSession == null) {
            this.firstSession = session;
        }
        return session;
    }

    @Override
    protected BedrockServerSession onSessionCreated(int sessionId) {
        BedrockServerSession session = (BedrockServerSession) super.onSessionCreated(sessionId);
        if (this.firstSession == null) {
            this.firstSession = session;
        }
        return session;
    }

    @Override
    protected void removeSession(BedrockSession session) {
        if (this.firstSession == session) {
            this.firstSession = null;
        }
        super.removeSession(session);
    }

    @Override
    protected void onTick() {
        if (!this.closed.get() && !this.packetQueue.isEmpty()) {
            BedrockBatchWrapper batch = BedrockBatchWrapper.newInstance();

            BedrockPacketWrapper packet;
            while ((packet = this.packetQueue.poll()) != null) {
                batch.getPackets().add(packet);
            }
            this.channel.writeAndFlush(batch);
        }
    }

    public void sendPacket(BedrockBatchWrapper wrapper) {
        if (this.channel.eventLoop().inEventLoop()) {
            this.sendPacket0(wrapper);
        } else {
            this.channel.eventLoop().execute(() -> this.sendPacket0(wrapper));
        }
    }

    private void sendPacket0(BedrockBatchWrapper wrapper) {
        if (!Objects.equals(wrapper.getAlgorithm(), this.compressionAlgorithm)) {
            wrapper.setCompressed(null);
        }
        this.onTick();
        this.getChannel().writeAndFlush(wrapper);
    }

    @Override
    public void sendPacketImmediately(int senderClientId, int targetClientId, BedrockPacket packet) {
        this.sendPacket(senderClientId, targetClientId, packet);
        if (this.channel.eventLoop().inEventLoop()) {
            this.onTick();
        } else {
            this.channel.eventLoop().execute(this::onTick);
        }
    }

    @Override
    public BedrockCodec getCodec() {
        return this.getChannel().pipeline().get(BedrockPacketCodec.class).getCodec();
    }

    @Override
    public BedrockCodecHelper getCodecHelper() {
        return this.getChannel().pipeline().get(BedrockPacketCodec.class).getHelper();
    }

    @Override
    public void setCodec(BedrockCodec codec) {
        Objects.requireNonNull(codec, "codec");
        this.getChannel().pipeline().get(BedrockPacketCodec.class).setCodecHelper(codec, codec.createHelper());
    }

    @Override
    public void enableEncryption(SecretKey secretKey) {
        Objects.requireNonNull(secretKey, "secretKey");
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

        log.info("Encryption enabled for {}", getSocketAddress());
    }

    @Override
    public void setCompression(PacketCompressionAlgorithm algorithm) {
        throw new UnsupportedOperationException("Use setCompression(BedrockCompressionAlgorithm algorithm) instead");
    }

    public void setCompression(CompressionAlgorithm algorithm) {
        Objects.requireNonNull(algorithm, "algorithm");
        ChannelHandler handler = this.channel.pipeline().get(CompressionCodec.NAME);
        if (handler instanceof NoopCompressionCodec) {
            this.channel.pipeline().remove(CompressionCodec.NAME);
        } else if (handler != null) {
            throw new IllegalArgumentException("Compression is already set");
        }
        this.channel.pipeline()
                .addAfter(FrameIdCodec.NAME, CompressionCodec.NAME, ProxiedSessionInitializer.getCompressionCodec(algorithm, this.getRakVersion(), false));
        this.compressionAlgorithm = algorithm;
    }

    public void disconnect(String reason) {
        this.sessions.values().forEach(session -> session.disconnect(reason));
        this.channel.eventLoop().schedule(() -> this.channel.close(), 200, TimeUnit.MILLISECONDS);
    }

    public int getRakVersion() {
        return this.channel.config().getOption(RakChannelOption.RAK_PROTOCOL_VERSION);
    }

    @Override
    public PacketCompressionAlgorithm getCompression() {
        return this.compressionAlgorithm.getBedrockAlgorithm();
    }

    public CompressionAlgorithm getCompressionAlgorithm() {
        return this.compressionAlgorithm;
    }

    public boolean isSplitScreen() {
        return this.sessions.size() > 1;
    }

    public long getPing() {
        if (this.channel instanceof RakChannel rakChannel) {
            return rakChannel.rakPipeline().get(RakSessionCodec.class).getPing();
        }
        return 0;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg instanceof BedrockBatchWrapper) {
                this.onBedrockBatch((BedrockBatchWrapper) msg);
            } else {
                super.channelRead(ctx, ReferenceCountUtil.retain(msg));
            }
        } catch (Exception e) {
            log.error("{} Exception caught in bedrock connection", ctx.channel().remoteAddress(), e);
            this.disconnect("Internal error");
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("{} Exception caught in bedrock connection", ctx.channel().remoteAddress(), cause);
        this.disconnect("Internal error");
    }
}
