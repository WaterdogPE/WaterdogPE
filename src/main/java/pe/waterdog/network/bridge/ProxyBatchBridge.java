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
import pe.waterdog.utils.exceptions.CancelSignalException;

import java.util.*;

public class ProxyBatchBridge implements BatchHandler {

    private final BedrockSession session;
    private final ProxiedPlayer player;

    public ProxyBatchBridge(ProxiedPlayer player, BedrockSession session){
        this.session = session;
        this.player = player;
    }

    @Override
    public void handle(BedrockSession session, ByteBuf buf, Collection<BedrockPacket> packets) {
        List<BedrockPacket> unhandledPackets =  new ArrayList<>();
        BedrockPacketHandler handler = session.getPacketHandler();
        boolean wrapperHandled = false;

        for (BedrockPacket packet : packets){
            this.player.getEntityMap().doRewrite(packet);

            if (!packet.handle(handler)){
                unhandledPackets.add(packet);
            }

            /*try {
                if (handler != null && ){
                    wrapperHandled = true;
                }else {
                    unhandledPackets.add(packet);
                }
            }catch (Exception e){
                //Use SneakyThrow to cancel sending packet
                if (e instanceof CancelSignalException){
                    wrapperHandled = true;
                }
            }*/
        }

        if (!unhandledPackets.isEmpty()) {
            this.session.sendWrapped(unhandledPackets, true);
        }

        /*if (!wrapperHandled) {
            buf.readerIndex(1); // FE - packet id
            this.session.sendWrapped(buf, this.session.isEncrypted());
        } else */
    }
}
