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

package pe.waterdog.network.upstream;

import com.nukkitx.protocol.bedrock.BedrockServerSession;
import com.nukkitx.protocol.bedrock.handler.BedrockPacketHandler;
import com.nukkitx.protocol.bedrock.packet.TextPacket;
import pe.waterdog.network.downstream.ServerInfo;
import pe.waterdog.player.ProxiedPlayer;

import java.net.InetSocketAddress;

public class UpstreamHandler implements BedrockPacketHandler {

    private final ProxiedPlayer player;
    private final BedrockServerSession session;

    public UpstreamHandler(ProxiedPlayer player, BedrockServerSession session){
        this.player = player;
        this.session = session;
    }

    @Override
    public boolean handle(TextPacket packet) {

        player.connect(new ServerInfo("lobby2", new InetSocketAddress("192.168.0.50", 19134)));
        return true;
    }
}
