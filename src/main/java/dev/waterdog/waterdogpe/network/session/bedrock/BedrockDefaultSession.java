/*
 * Copyright 2021 WaterdogTEAM
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

package dev.waterdog.waterdogpe.network.session.bedrock;

import com.google.common.base.Preconditions;
import com.nukkitx.protocol.bedrock.BedrockClientSession;
import com.nukkitx.protocol.bedrock.BedrockPacket;
import dev.waterdog.waterdogpe.WaterdogPE;
import dev.waterdog.waterdogpe.network.bridge.TransferBatchBridge;
import dev.waterdog.waterdogpe.network.downstream.ConnectedDownstreamHandler;
import dev.waterdog.waterdogpe.network.downstream.InitialHandler;
import dev.waterdog.waterdogpe.network.downstream.SwitchDownstreamHandler;
import dev.waterdog.waterdogpe.network.protocol.ProtocolVersion;
import dev.waterdog.waterdogpe.network.session.CompressionAlgorithm;
import dev.waterdog.waterdogpe.network.session.DownstreamSession;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import io.netty.buffer.ByteBuf;

import javax.crypto.SecretKey;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.function.Consumer;

public class BedrockDefaultSession implements DownstreamSession {

    private final BedrockDefaultClient client;
    private final BedrockClientSession session;

    private CompressionAlgorithm compression = CompressionAlgorithm.ZLIB;

    public BedrockDefaultSession(BedrockDefaultClient client, BedrockClientSession session) {
        this.client = client;
        this.session = session;
    }

    @Override
    public void onDownstreamInit(ProxiedPlayer player, boolean initial) {
        this.session.setCompressionLevel(player.getProxy().getConfiguration().getDownstreamCompression());
        this.session.setPacketCodec(player.getProtocol().getCodec());
        this.session.setLogging(WaterdogPE.version().debug());

        // Disable compression before we receive NetworkSettings from server
        if (player.getProtocol().isAfterOrEqual(ProtocolVersion.MINECRAFT_PE_1_19_30)) {
            this.compression = CompressionAlgorithm.NONE;
        }

        if (initial) {
            this.session.setPacketHandler(new InitialHandler(player, this.client));
            this.session.setBatchHandler(new BedrockDownstreamBridge(player, player.getUpstream(), this));
        } else {
            this.session.setPacketHandler(new SwitchDownstreamHandler(player, this.client));
            this.session.setBatchHandler(new BedrockTransferBatchBridge(player, player.getUpstream(), this));
            // Make sure TransferBatchBridge queue is released and there is no memory leak
            this.session.addDisconnectHandler(reason -> TransferBatchBridge.release(this.session.getBatchHandler()));
        }
    }

    @Override
    public void onInitialServerConnected(ProxiedPlayer player) {
        this.session.setPacketHandler(new ConnectedDownstreamHandler(player, this.client));
    }

    @Override
    public void onServerConnected(ProxiedPlayer player) {
        TransferBatchBridge batchBridge = this.getBatchBridge();
        if (batchBridge != null) {
            batchBridge.setDimLockActive(true);
        }
    }

    @Override
    public void onTransferCompleted(ProxiedPlayer player, Runnable completedCallback) {
        // Allow transfer queue to be sent
        TransferBatchBridge batchBridge = this.getBatchBridge();
        if (batchBridge != null) {
            batchBridge.setDimLockActive(false);
        }

        // Change downstream bridge on same eventLoop as packet are being processed on to
        // prevent packet reordering in some situations
        if (this.session.getEventLoop().inEventLoop()) {
            this.onTransferCompleted0(player, completedCallback);
        } else {
            this.session.getEventLoop().execute(() -> this.onTransferCompleted0(player, completedCallback));
        }
    }

    private void onTransferCompleted0(ProxiedPlayer player, Runnable completedCallback) {
        TransferBatchBridge batchBridge = this.getBatchBridge();
        this.session.setBatchHandler(new BedrockDownstreamBridge(player, player.getUpstream(), this));
        this.session.setPacketHandler(new ConnectedDownstreamHandler(player, this.client));

        if (batchBridge != null) {
            batchBridge.flushQueue();
        }

        completedCallback.run();
    }

    private TransferBatchBridge getBatchBridge() {
        if (this.session.getBatchHandler() instanceof TransferBatchBridge) {
            return (TransferBatchBridge) this.session.getBatchHandler();
        }
        return null;
    }

    @Override
    public void setCompression(CompressionAlgorithm compression) {
        Preconditions.checkArgument(this.compression == null || this.compression == CompressionAlgorithm.NONE, "Compression was already set");
        this.compression = compression;
        this.session.setCompression(compression.getBedrockCompression());
    }

    @Override
    public CompressionAlgorithm getCompression() {
        return this.compression;
    }

    @Override
    public void addDisconnectHandler(Consumer<Object> handler) {
        this.session.addDisconnectHandler(handler::accept);
    }

    @Override
    public void sendPacket(BedrockPacket packet) {
        this.session.sendPacket(packet);
    }

    @Override
    public void sendPacketImmediately(BedrockPacket packet) {
        this.session.sendPacketImmediately(packet);
    }

    @Override
    public void sendWrapped(Collection<BedrockPacket> packets, boolean encrypt) {
        this.session.sendWrapped(packets, encrypt);
    }

    @Override
    public void sendWrapped(ByteBuf compressed, boolean encrypt) {
        this.session.sendWrapped(compressed, encrypt);
    }

    @Override
    public void enableEncryption(SecretKey secretKey) {
        this.session.enableEncryption(secretKey);
    }

    @Override
    public boolean isEncrypted() {
        return this.session.isEncrypted();
    }

    @Override
    public int getHardcodedBlockingId() {
        return this.session.getHardcodedBlockingId().get();
    }

    @Override
    public void disconnect() {
        this.session.disconnect();
    }

    @Override
    public InetSocketAddress getAddress() {
        return this.session.getRealAddress();
    }

    @Override
    public long getLatency() {
        return this.session.getLatency();
    }

    @Override
    public boolean isClosed() {
        return this.session.isClosed();
    }

    public BedrockClientSession getSession() {
        return this.session;
    }
}
