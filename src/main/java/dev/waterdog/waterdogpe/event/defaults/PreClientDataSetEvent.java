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

import java.security.KeyPair;

/**
 * Called right when we decoded the player's LoginPacket data in the handshake(HandshakeUpstreamHandler).
 * Can be used to modify or filter (for) certain data, for example skin data.
 */
public class PreClientDataSetEvent extends Event {

    private final ProxiedConnection connection;
    private final JsonObject clientData;
    private final JsonObject extraData;
    private KeyPair keyPair;

    public PreClientDataSetEvent(JsonObject clientData, JsonObject extraData, KeyPair keyPair, ProxiedConnection playerSession) {
        this.clientData = clientData;
        this.extraData = extraData;
        this.connection = playerSession;
        this.keyPair = keyPair;
    }

    public ProxiedConnection getConnection() {
        return this.connection;
    }

    public JsonObject getClientData() {
        return this.clientData;
    }

    public JsonObject getExtraData() {
        return this.extraData;
    }

    public KeyPair getKeyPair() {
        return this.keyPair;
    }

    public void setKeyPair(KeyPair keyPair) {
        this.keyPair = keyPair;
    }
}
