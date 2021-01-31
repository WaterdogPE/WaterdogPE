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

package dev.waterdog.command;

import dev.waterdog.ProxyServer;
import dev.waterdog.utils.types.TextContainer;

/**
 * Base interface for all instances that are able to issue commands.
 */
public interface CommandSender {

    String getName();

    boolean isPlayer();

    boolean hasPermission(String permission);

    void sendMessage(String message);

    void sendMessage(TextContainer message);

    ProxyServer getProxy();
}
