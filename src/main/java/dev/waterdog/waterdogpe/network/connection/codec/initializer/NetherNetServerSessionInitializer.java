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

package dev.waterdog.waterdogpe.network.connection.codec.initializer;

import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.network.NetworkMetrics;
import dev.waterdog.waterdogpe.network.connection.TransportProfile;
import dev.waterdog.waterdogpe.network.connection.codec.server.ServerErrorHandler;
import dev.waterdog.waterdogpe.network.connection.peer.BedrockServerSession;
import dev.waterdog.waterdogpe.network.connection.peer.ProxiedBedrockPeer;
import dev.waterdog.waterdogpe.network.protocol.handler.upstream.LoginUpstreamHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import org.cloudburstmc.protocol.bedrock.BedrockPeer;
import org.cloudburstmc.protocol.bedrock.PacketDirection;

/**
 * Upstream sessions arriving over NetherNet.
 * <p>
 * Identical to the RakNet path above the frame codec: the data channel payload is the same batch
 * body, minus the frame id, so compression, encryption, batching and the packet codec are shared.
 */
public class NetherNetServerSessionInitializer extends ProxiedSessionInitializer<BedrockServerSession> {

    public NetherNetServerSessionInitializer(ProxyServer proxy) {
        super(proxy);
    }

    @Override
    protected void initChannel(Channel channel) {
        // The security manager already saw this connection when its offer arrived, running it
        // again here would charge the connection throttle twice for one join.
        channel.attr(PacketDirection.ATTRIBUTE).set(PacketDirection.CLIENT_BOUND);

        NetworkMetrics metrics = this.proxy.getNetworkMetrics();
        if (metrics != null) {
            channel.attr(NetworkMetrics.ATTRIBUTE).set(metrics);
        }

        super.initChannel(channel);

        channel.pipeline().addLast(ServerErrorHandler.NAME, new ServerErrorHandler.Child(this.proxy));
    }

    @Override
    protected ChannelHandler getFrameCodec() {
        return NETHERNET_FRAME_CODEC;
    }

    @Override
    protected TransportProfile getTransportProfile(Channel channel) {
        return TransportProfile.NETHERNET;
    }

    @Override
    protected BedrockServerSession createSession0(BedrockPeer peer, int subClientId) {
        this.proxy.getLogger().debug("[" + peer.getSocketAddress() + "] <-> Received first data over NetherNet");
        return new BedrockServerSession((ProxiedBedrockPeer) peer, subClientId);
    }

    @Override
    protected void initSession(BedrockServerSession session) {
        session.setPacketHandler(new LoginUpstreamHandler(this.proxy, session));
    }
}
