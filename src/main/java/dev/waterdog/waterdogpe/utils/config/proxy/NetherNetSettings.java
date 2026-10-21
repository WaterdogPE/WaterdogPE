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

package dev.waterdog.waterdogpe.utils.config.proxy;

import lombok.Getter;
import net.cubespace.Yamler.Config.Comment;
import net.cubespace.Yamler.Config.Comments;
import net.cubespace.Yamler.Config.Path;

import java.util.Set;

/**
 * NetherNet options, named like Geyser's: webrtc_port is its bedrock.webrtc-port and signaling its
 * bedrock.signaling. Whether NetherNet runs at all is {@link ProxyConfig#getTransport()}.
 */
@Getter
public class NetherNetSettings extends SettingsSection {

    @Path("webrtc_port")
    @Comments({
            "Dedicated UDP port for NetherNet media, one socket for every peer, which STUN discovery",
            "shares. While RakNet runs it must not be the listener port, which RakNet holds. 0 picks the",
            "listener port with listener.transport nethernet, otherwise the first port after it that no",
            "server in servers uses, which the startup log names. Players connect to it directly, so",
            "forward it like the listener port when behind a router. With NXS signaling and assisted_joins",
            "off, they cannot connect otherwise."
    })
    private int webrtcPort = 0;

    @Path("signaling")
    @Comment("How players find this proxy to open a NetherNet connection to it")
    private SignalingSettings signaling = new SignalingSettings();

    @Path("identity_file")
    @Comments({
            "Unencrypted PEM private key holding the P-384 key identifying this operator to",
            "clients, generated on first start. Share it across a fleet to be trusted as one",
            "operator; replacing it re-prompts every returning player. An absolute path works,",
            "so it can be mounted from outside the proxy directory."
    })
    private String identityFile = "nethernet/identity.pem";

    /**
     * The port media binds for a listener: webrtc_port, or for 0 the listener port when RakNet does
     * not hold it, otherwise the first port after it that no server uses, as a server on this machine
     * would already hold it.
     *
     * @param raknet      Whether RakNet runs on the listener port
     * @param serverPorts The ports of the servers in the server list
     */
    public int resolveWebrtcPort(int listenerPort, boolean raknet, Set<Integer> serverPorts) {
        if (this.webrtcPort > 0) {
            return this.webrtcPort;
        }
        if (!raknet) {
            return listenerPort;
        }
        int port = listenerPort + 1;
        while (port <= 65535 && serverPorts.contains(port)) {
            port++;
        }
        if (port > 65535) {
            port = listenerPort - 1;
            while (serverPorts.contains(port)) {
                port--;
            }
        }
        return port;
    }
}
