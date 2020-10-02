/**
 * Copyright 2020 WaterdogTEAM
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pe.waterdog.network;

import com.nukkitx.protocol.bedrock.BedrockPong;
import com.nukkitx.protocol.bedrock.BedrockServerEventHandler;
import com.nukkitx.protocol.bedrock.BedrockServerSession;
import pe.waterdog.ProxyServer;
import pe.waterdog.network.protocol.ProtocolConstants;
import pe.waterdog.network.upstream.HandshakeUpstreamHandler;
import pe.waterdog.utils.ProxyConfig;

import java.net.InetSocketAddress;

public class ProxyListener implements BedrockServerEventHandler {

    private static final ThreadLocal<BedrockPong> PONG_THREAD_LOCAL = ThreadLocal.withInitial(BedrockPong::new);

    private ProxyServer proxy;

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
        //TODO: query event here

        BedrockPong pong = PONG_THREAD_LOCAL.get();
        pong.setEdition("MCPE");
        pong.setMotd(config.getMotd());
        pong.setSubMotd("");
        pong.setGameType("SMP");
        pong.setMaximumPlayerCount(config.getMaxPlayerCount());
        pong.setPlayerCount(0);
        pong.setIpv4Port(config.getBindAddress().getPort());
        pong.setIpv6Port(config.getBindAddress().getPort());
        pong.setProtocolVersion(ProtocolConstants.Protocol.MINECRAFT_PE_1_13.getProtocol());
        pong.setVersion("");
        pong.setNintendoLimited(false);

        return pong;
    }

    @Override
    public void onSessionCreation(BedrockServerSession session) {
        this.proxy.getLogger().info("[" + session.getAddress() + "] <-> InitialHandler has connected");
        session.setPacketHandler(new HandshakeUpstreamHandler(this.proxy, session));
    }

}
