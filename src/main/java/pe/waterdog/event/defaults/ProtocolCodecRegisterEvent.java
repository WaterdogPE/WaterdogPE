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

package pe.waterdog.event.defaults;

import com.nukkitx.protocol.bedrock.BedrockPacketCodec;
import pe.waterdog.event.CancellableEvent;
import pe.waterdog.event.Event;
import pe.waterdog.network.protocol.ProtocolVersion;

public class ProtocolCodecRegisterEvent extends Event implements CancellableEvent {

    private final ProtocolVersion protocolVersion;
    private final BedrockPacketCodec.Builder codecBuilder;

    public ProtocolCodecRegisterEvent(ProtocolVersion protocolVersion, BedrockPacketCodec.Builder codecBuilder){
        this.protocolVersion = protocolVersion;
        this.codecBuilder = codecBuilder;
    }

    public ProtocolVersion getProtocolVersion() {
        return this.protocolVersion;
    }

    public BedrockPacketCodec.Builder getCodecBuilder() {
        return this.codecBuilder;
    }
}
