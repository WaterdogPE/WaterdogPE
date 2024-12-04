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

import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.network.NetworkMetrics;
import dev.waterdog.waterdogpe.network.connection.peer.BedrockServerSession;
import dev.waterdog.waterdogpe.network.connection.peer.ProxiedBedrockPeer;
import dev.waterdog.waterdogpe.network.protocol.handler.upstream.LoginUpstreamHandler;
import io.netty.channel.Channel;
import org.cloudburstmc.netty.channel.raknet.RakChannel;
import org.cloudburstmc.netty.channel.raknet.RakDisconnectReason;
import org.cloudburstmc.netty.channel.raknet.config.RakChannelMetrics;
import org.cloudburstmc.netty.channel.raknet.config.RakChannelOption;
import org.cloudburstmc.netty.handler.codec.raknet.common.RakSessionCodec;
import org.cloudburstmc.protocol.bedrock.BedrockPeer;
import org.cloudburstmc.protocol.bedrock.PacketDirection;

public class ProxiedServerSessionInitializer extends ProxiedSessionInitializer<BedrockServerSession> {

    public ProxiedServerSessionInitializer(ProxyServer proxy) {
        super(proxy);
    }

    @Override
    protected void initChannel(Channel channel) {
        if (!this.proxy.getSecurityManager().onConnectionCreated(channel.remoteAddress())) {
            this.proxy.getLogger().info("[" + channel.remoteAddress() + "] <-> Connection request denied");
            disconnect(channel, RakDisconnectReason.DISCONNECTED);
            return;
        }

        channel.attr(PacketDirection.ATTRIBUTE).set(PacketDirection.CLIENT_BOUND);

        NetworkMetrics metrics = this.proxy.getNetworkMetrics();
        if (metrics != null) {
            channel.attr(NetworkMetrics.ATTRIBUTE).set(metrics);
        }
        if (metrics instanceof RakChannelMetrics rakMetrics) {
            channel.config().setOption(RakChannelOption.RAK_METRICS, rakMetrics);
        }

        super.initChannel(channel);
    }

    @Override
    protected BedrockServerSession createSession0(BedrockPeer peer, int subClientId) {
        this.proxy.getLogger().debug("[" + peer.getSocketAddress() + "] <-> Received first data");
        return new BedrockServerSession((ProxiedBedrockPeer) peer, subClientId);
    }

    @Override
    protected void initSession(BedrockServerSession session) {
        session.setPacketHandler(new LoginUpstreamHandler(this.proxy, session));
    }

    private static void disconnect(Channel channel, RakDisconnectReason reason) {
        if (channel instanceof RakChannel) {
            ((RakChannel) channel).rakPipeline().get(RakSessionCodec.class).disconnect(reason);
        } else {
            channel.disconnect();
        }
    }
}
