/*
 * Copyright 2026 WaterdogTEAM
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

package dev.waterdog.waterdogpe.network.protocol;

import dev.waterdog.waterdogpe.network.connection.ProxiedConnection;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerListPacket;

@Log4j2
public class PacketUtils {

  /** Names every packet crossing the proxy, for working out where one goes missing. */
  public static final boolean TRACE_PACKETS = Boolean.getBoolean("waterdog.packetTrace");

  /**
   * Names a packet the proxy sends a server itself, which the relay trace never shows.
   */
  public static void traceSentToServer(ServerInfo server, BedrockPacket packet) {
    if (TRACE_PACKETS) {
      log.info("[proxy -> server {}] {}", server.getServerName(), packet.getPacketType());
    }
  }

  /**
   * Names a packet the proxy sends the client itself, which the relay trace never shows.
   */
  public static void traceSentToClient(ProxiedConnection client, BedrockPacket packet) {
    if (TRACE_PACKETS) {
      log.info("[proxy -> client {}] {}", client.getSocketAddress(), packet.getPacketType());
    }
  }

  public static PlayerListPacket.Action getAction(PlayerListPacket packet, PlayerListPacket.Entry entry) {
    return entry.getAction() != null ? entry.getAction() : packet.getAction();
  }

}
