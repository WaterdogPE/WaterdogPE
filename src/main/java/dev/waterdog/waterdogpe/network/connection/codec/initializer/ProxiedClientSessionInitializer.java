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

package dev.waterdog.waterdogpe.network.connection.codec.initializer;

import dev.waterdog.waterdogpe.network.connection.client.BedrockClientConnection;
import dev.waterdog.waterdogpe.network.connection.client.ClientConnection;
import dev.waterdog.waterdogpe.network.connection.codec.batch.BedrockBatchDecoder;
import dev.waterdog.waterdogpe.network.connection.codec.batch.BedrockBatchEncoder;
import dev.waterdog.waterdogpe.network.connection.codec.batch.FrameIdCodec;
import dev.waterdog.waterdogpe.network.connection.codec.client.ClientEventHandler;
import dev.waterdog.waterdogpe.network.connection.codec.compression.CompressionAlgorithm;
import dev.waterdog.waterdogpe.network.connection.codec.packet.BedrockPacketCodec;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.util.concurrent.Promise;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.protocol.bedrock.netty.codec.compression.CompressionCodec;

import static dev.waterdog.waterdogpe.network.connection.codec.initializer.ProxiedSessionInitializer.*;

public class ProxiedClientSessionInitializer extends ChannelInitializer<Channel> {
    private final ProxiedPlayer player;
    private final ServerInfo serverInfo;
    private final Promise<ClientConnection> promise;

    public ProxiedClientSessionInitializer(ProxiedPlayer player, ServerInfo serverInfo, Promise<ClientConnection> promise) {
        this.player = player;
        this.serverInfo = serverInfo;
        this.promise = promise;
    }

    @Override
    protected void initChannel(Channel channel) throws Exception {
        int rakVersion = this.player.getProtocol().getRaknetVersion();
        CompressionAlgorithm compression = this.player.getProxy().getConfiguration().getCompression();

        channel.pipeline()
                .addLast(FrameIdCodec.NAME, RAKNET_FRAME_CODEC)
                .addLast(CompressionCodec.NAME, getCompressionCodec(compression, rakVersion, true))
                .addLast(BedrockBatchDecoder.NAME, BATCH_DECODER)
                .addLast(BedrockBatchEncoder.NAME, new BedrockBatchEncoder())
                .addLast(BedrockPacketCodec.NAME, getPacketCodec(rakVersion));

        BedrockClientConnection connection = new BedrockClientConnection(this.player, this.serverInfo, channel);
        channel.pipeline()
                .addLast(ClientConnection.NAME, connection)
                .addLast(ClientEventHandler.NAME, new ClientEventHandler(this.player, connection))
                .addLast(new ChannelActiveHandler(connection, this.promise)); // this should be the very last handler
    }

    @RequiredArgsConstructor
    private static class ChannelActiveHandler extends ChannelInboundHandlerAdapter {
        private final BedrockClientConnection connection;
        private final Promise<ClientConnection> promise;

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            this.promise.trySuccess(this.connection);
            ctx.channel().pipeline().remove(this);
        }
    }
}
