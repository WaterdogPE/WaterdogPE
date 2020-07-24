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
import com.nukkitx.protocol.bedrock.packet.*;
import lombok.SneakyThrows;
import pe.waterdog.ProxyServer;
import pe.waterdog.network.ServerInfo;
import pe.waterdog.network.bridge.ProxyBatchBridge;
import pe.waterdog.player.PlayerRewriteUtils;
import pe.waterdog.player.ProxiedPlayer;

public class UpstreamHandler implements BedrockPacketHandler {

    private final ProxiedPlayer player;
    private final BedrockServerSession session;

    public UpstreamHandler(ProxiedPlayer player, BedrockServerSession session){
        this.player = player;
        this.session = session;
    }

    @SneakyThrows
    @Override
    public boolean handle(TextPacket packet) {
        if (!packet.getMessage().startsWith("server")) return true;

        String[] args = packet.getMessage().split(" ");
        if (args.length <= 1) return true;

        ServerInfo serverInfo = ProxyServer.getInstance().getServer(args[1]);
        if (serverInfo != null){
            player.connect(serverInfo);
            //throw CancelSignalException.CANCEL;
        }

        return true;
    }

    @Override
    public boolean handle(PlayerActionPacket packet) {
        if (packet.getAction() != PlayerActionPacket.Action.DIMENSION_CHANGE_SUCCESS || !this.player.isDimensionChange()) return true;

        PlayerRewriteUtils.injectDimensionChange(player.getUpstream(), player.getRewriteData().getDimension());
        this.player.setDimensionChange(false);
        return true;
    }
}
