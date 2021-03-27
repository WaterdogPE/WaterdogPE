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

package dev.waterdog.network;

import com.nukkitx.protocol.bedrock.BedrockPong;
import com.nukkitx.protocol.bedrock.BedrockServerEventHandler;
import com.nukkitx.protocol.bedrock.BedrockServerSession;
import dev.waterdog.ProxyServer;
import dev.waterdog.event.defaults.ProxyPingEvent;
import dev.waterdog.network.protocol.ProtocolConstants;
import dev.waterdog.network.upstream.HandshakeUpstreamHandler;
import dev.waterdog.query.QueryHandler;
import dev.waterdog.utils.ProxyConfig;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;

import java.net.InetSocketAddress;
import java.util.Arrays;

/**
 * Main class for Proxy-related traffic.
 * Handles Bedrock Queries as well as new incoming connections.
 */
public class ProxyListener implements BedrockServerEventHandler {

    private static final ThreadLocal<BedrockPong> PONG_THREAD_LOCAL = ThreadLocal.withInitial(BedrockPong::new);

    private final ProxyServer proxy;

    public ProxyListener(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Override
    public boolean onConnectionRequest(InetSocketAddress address) {
        return true;
    }

    @Override
    public BedrockPong onQuery(InetSocketAddress address) {
        ProxyConfig config = this.proxy.getConfiguration();

        ProxyPingEvent event = new ProxyPingEvent(
                config.getMotd(),
                "WaterdogPE Proxy",
                "Survival",
                "MCPE",
                ProtocolConstants.getLatestProtocol().getMinecraftVersion(),
                this.proxy.getPlayerManager().getPlayers().values(),
                config.getMaxPlayerCount(),
                address
        );
        this.proxy.getEventManager().callEvent(event);

        BedrockPong pong = PONG_THREAD_LOCAL.get();
        pong.setEdition(event.getEdition());
        pong.setMotd(event.getMotd());
        pong.setSubMotd(event.getSubMotd());
        pong.setGameType(event.getGameType());
        pong.setMaximumPlayerCount(event.getMaximumPlayerCount());
        pong.setPlayerCount(event.getPlayerCount());
        pong.setIpv4Port(config.getBindAddress().getPort());
        pong.setIpv6Port(config.getBindAddress().getPort());
        pong.setProtocolVersion(ProtocolConstants.getLatestProtocol().getProtocol());
        pong.setVersion(event.getVersion());
        pong.setNintendoLimited(false);
        return pong;
    }

    @Override
    public void onSessionCreation(BedrockServerSession session) {
        this.proxy.getLogger().debug("[" + session.getAddress() + "] <-> Received first data");
        session.setPacketHandler(new HandshakeUpstreamHandler(this.proxy, session));
    }

    @Override
    public void onUnhandledDatagram(ChannelHandlerContext ctx, DatagramPacket packet) {
        ByteBuf buf = packet.content();
        if (!buf.isReadable(7)) {
            return;
        }

        try {
            byte[] prefix = new byte[2];
            buf.readBytes(prefix);

            QueryHandler queryHandler = this.proxy.getQueryHandler();
            if (queryHandler != null && Arrays.equals(prefix, QueryHandler.QUERY_SIGNATURE)) {
                queryHandler.onQuery(packet.sender(), buf, ctx);
            }
        } catch (Exception e) {
            this.proxy.getLogger().error("Can not handle packet!", e);
        }
    }
}
