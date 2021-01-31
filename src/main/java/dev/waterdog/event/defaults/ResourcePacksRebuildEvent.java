/*
 * Copyright 2021 WaterdogTEAM
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.waterdog.event.defaults;

import com.nukkitx.protocol.bedrock.packet.ResourcePackStackPacket;
import com.nukkitx.protocol.bedrock.packet.ResourcePacksInfoPacket;
import dev.waterdog.event.AsyncEvent;
import dev.waterdog.event.Event;

@AsyncEvent
public class ResourcePacksRebuildEvent extends Event {

    private final ResourcePacksInfoPacket packsInfoPacket;
    private final ResourcePackStackPacket stackPacket;

    public ResourcePacksRebuildEvent(ResourcePacksInfoPacket packsInfoPacket, ResourcePackStackPacket stackPacket) {
        this.packsInfoPacket = packsInfoPacket;
        this.stackPacket = stackPacket;
    }

    public ResourcePacksInfoPacket getPacksInfoPacket() {
        return this.packsInfoPacket;
    }

    public ResourcePackStackPacket getStackPacket() {
        return this.stackPacket;
    }
}
