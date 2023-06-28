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

package dev.waterdog.waterdogpe.command;

import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.utils.types.TextContainer;

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
