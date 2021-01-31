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

import dev.waterdog.event.CancellableEvent;
import dev.waterdog.player.ProxiedPlayer;

/**
 * Called before a player send a message to the chat.
 * At this point it is possible to cancel or modify the message.
 */
public class PlayerChatEvent extends PlayerEvent implements CancellableEvent {

    private String message;

    public PlayerChatEvent(ProxiedPlayer player, String message) {
        super(player);
        this.message = message;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
