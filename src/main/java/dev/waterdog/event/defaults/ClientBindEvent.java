/*
 * Copyright 2021 WaterdogTEAM
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

package dev.waterdog.event.defaults;

import com.nukkitx.protocol.bedrock.BedrockClient;
import dev.waterdog.ProxyServer;
import dev.waterdog.event.AsyncEvent;
import dev.waterdog.event.Event;
import dev.waterdog.player.ProxiedPlayer;

/**
 * Called when new downstream client for player is created and bind.
 */
public class ClientBindEvent extends PlayerEvent {

    private final BedrockClient client;

    public ClientBindEvent(ProxiedPlayer player, BedrockClient client) {
        super(player);
        this.client = client;
    }

    public BedrockClient getClient() {
        return this.client;
    }
}
