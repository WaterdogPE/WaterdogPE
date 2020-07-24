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
import pe.waterdog.logger.Logger;
import pe.waterdog.player.ProxiedPlayer;
import pe.waterdog.utils.exceptions.CancelSignalException;

import java.util.*;

public class ProxyBatchBridgeOld implements BatchHandler {

    private final BedrockSession session;
    private final ProxiedPlayer player;

    protected final Queue<ByteBuf> queue = new ArrayDeque<>(42);
    protected boolean isLocked = false;

    public ProxyBatchBridgeOld(ProxiedPlayer player, BedrockSession session){
        this.session = session;
        this.player = player;
    }

    @Override
    public void handle(BedrockSession session, ByteBuf buf, Collection<BedrockPacket> packets) {
        List<BedrockPacket> unhandledPackets =  new ArrayList<>();
        BedrockPacketHandler handler = session.getPacketHandler();
        boolean wrapperHandled = false;

        if (isLocked){
            queue.add(buf);
            return;
        }

        for (BedrockPacket packet : packets){
            this.player.getEntityMap().doRewrite(packet);

            /*if (!(packet instanceof NetworkStackLatencyPacket)) {
                Logger.getLogger().info(session.getAddress() +" <-> "+packet);
            }*/

            /*Send original buffer when packet was handled*/
            try {
                if (handler != null && packet.handle(handler)){
                    wrapperHandled = true;
                    continue;
                }
            }catch (Exception e){
                if (e instanceof CancelSignalException){ //Use SneakyThrow to cancel sending packet
                    wrapperHandled = false;
                    continue;
                }
            }

            unhandledPackets.add(packet);
        }

        if (!wrapperHandled){
            this.session.sendWrapped(unhandledPackets, true);
            return;
        }

        buf.readerIndex(1);
        this.session.sendWrapped(buf, this.session.isEncrypted());
    }

    private void doUnlock(){
        while (!queue.isEmpty() && !this.isLocked){
            ByteBuf buf = queue.remove();
            buf.readerIndex(1);
            this.session.sendWrapped(buf, this.session.isEncrypted());
        }
    }

    public void setLocked(boolean locked) {
        this.isLocked = locked;
        Logger.getLogger().info("Set lock: "+locked);
        if (!locked) this.doUnlock();
    }

    public boolean isLocked() {
        return this.isLocked;
    }
}
