/*
 * Copyright 2022 WaterdogTEAM
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

package dev.waterdog.waterdogpe.event.defaults;

import com.google.gson.JsonObject;
import dev.waterdog.waterdogpe.event.Event;
import dev.waterdog.waterdogpe.network.connection.ProxiedConnection;
import lombok.Getter;
import lombok.Setter;

import java.security.KeyPair;
import java.util.UUID;

/**
 * Called right when we decoded the player's LoginPacket data in the handshake(HandshakeUpstreamHandler).
 * Can be used to modify or filter (for) certain data, for example skin data.
 */
public class PreClientDataSetEvent extends Event {

    @Getter
    private final ProxiedConnection connection;
    @Getter
    private final JsonObject clientData;
    @Getter
    private final String xuid;
    @Getter
    private final UUID uuid;
    @Getter
    private final String displayName;
    @Setter
    @Getter
    private KeyPair keyPair;

    public PreClientDataSetEvent(JsonObject clientData, String xuid, UUID uuid, String displayName, KeyPair keyPair, ProxiedConnection playerSession) {
        this.clientData = clientData;
        this.xuid = xuid;
        this.uuid = uuid;
        this.displayName = displayName;
        this.connection = playerSession;
        this.keyPair = keyPair;
    }
}
