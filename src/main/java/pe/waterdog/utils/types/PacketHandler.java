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

package pe.waterdog.utils.types;

import com.nukkitx.protocol.bedrock.BedrockPacket;
import com.nukkitx.protocol.bedrock.BedrockSession;
import com.nukkitx.protocol.bedrock.handler.BedrockPacketHandler;

/**
 * Using PacketHandler class plugins can handle safely handle packet.
 * Handle method will be only invoked if packet was not canceled by proxy handlers.
 */
public abstract class PacketHandler implements BedrockPacketHandler {

    private final BedrockSession session;

    public PacketHandler(BedrockSession session) {
        this.session = session;
    }

    public boolean handlePacket(BedrockPacket packet) {
        return packet.handle(this);
    }

    public BedrockSession getSession() {
        return this.session;
    }
}
