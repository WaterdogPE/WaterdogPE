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

package pe.waterdog.network.bridge;

import com.nukkitx.protocol.bedrock.BedrockPacket;
import com.nukkitx.protocol.bedrock.BedrockSession;
import com.nukkitx.protocol.bedrock.handler.BatchHandler;
import com.nukkitx.protocol.bedrock.handler.BedrockPacketHandler;
import io.netty.buffer.ByteBuf;
import pe.waterdog.player.ProxiedPlayer;

import java.util.*;

public class ProxyBatchBridge implements BatchHandler {

    protected final BedrockSession session;
    protected final ProxiedPlayer player;

    public ProxyBatchBridge(ProxiedPlayer player, BedrockSession session){
        this.session = session;
        this.player = player;
    }

    @Override
    public void handle(BedrockSession session, ByteBuf buf, Collection<BedrockPacket> packets) {
        List<BedrockPacket> newPackets =  new ArrayList<>();
        BedrockPacketHandler handler = session.getPacketHandler();

        for (BedrockPacket packet : packets){
            if (this.sendPacket(packet, handler)){
                newPackets.add(packet);
            }
        }

        if (!newPackets.isEmpty()) {
            this.session.sendWrapped(newPackets, true);
        }
    }

    public boolean sendPacket(BedrockPacket packet, BedrockPacketHandler handler){
        boolean unhandled = !packet.handle(handler);
        return this.player.getEntityMap().doRewrite(packet) || unhandled;
    }
}
