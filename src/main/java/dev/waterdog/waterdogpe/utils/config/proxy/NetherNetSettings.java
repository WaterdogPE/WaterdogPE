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
import lombok.Setter;
import lombok.experimental.Accessors;
import net.cubespace.Yamler.Config.Comment;
import net.cubespace.Yamler.Config.Comments;
import net.cubespace.Yamler.Config.Path;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Getter
public class NetherNetSettings extends SettingsSection {

    /**
     * How the proxy obtains SDP offers from clients.
     */
    public enum SignalingMode {
        /**
         * The proxy runs the HTTP signaling endpoint itself.
         */
        BUILTIN,
        /**
         * The proxy registers with the NXS provider, which hands it admitted connections.
         */
        NXS,
        /**
         * Both of the above at once, so players can arrive either way.
         */
        HYBRID,
        /**
         * Nothing is bound. Offers arrive through the signaling API, driven by a plugin.
         */
        PLUGIN;

        public boolean builtin() {
            return this == BUILTIN || this == HYBRID;
        }

        public boolean nxs() {
            return this == NXS || this == HYBRID;
        }
    }

    @Path("enabled")
    @Accessors(fluent = true)
    @Comment("Accept NetherNet connections alongside RakNet")
    private boolean enabled = false;

    @Path("signaling_mode")
    @Comments({
            "builtin: the proxy serves the HTTP signaling endpoint on the listener port",
            "nxs: the proxy registers with the provider configured below and it hands over players",
            "hybrid: both, so players can arrive either way",
            "plugin: nothing is bound, a plugin feeds offers through the signaling API"
    })
    @Setter
    private String signalingMode = "builtin";

    @Path("nxs")
    @Comment("Settings for the NXS signaling provider. Only used in the nxs and hybrid modes")
    private NxsSettings nxs = new NxsSettings();

    @Path("trusted_proxies")
    @Comments({
            "Addresses allowed to set X-Forwarded-For on signaling requests, as hosts or CIDR ranges",
            "such as 10.0.0.0/8. An http or https entry is fetched and read as one address per line.",
            "Needed when a reverse proxy fronts the signaling port, so the real client address survives."
    })
    private List<String> trustedProxies = new ArrayList<>();

    @Path("proxy_protocol")
    @Comments({
            "Read a HAProxy PROXY header, v1 or v2, on signaling connections from a trusted_proxies",
            "address. Connections without one are still served and fall back to X-Forwarded-For."
    })
    private boolean proxyProtocol = false;

    @Path("advertise_addresses")
    @Comments({
            "Addresses players connect to for NetherNet media, for built-in signaling and NXS alike: IP",
            "addresses or host names, such as play.example.com or [2001:db8::1]:19133. One without a",
            "port uses udp_port. A host name is looked up when signaling starts, which fails if it does",
            "not resolve. An address this machine does not hold is announced as the public side of a",
            "NAT forwarding that port here. Media bypasses a signaling reverse proxy, so its address",
            "only belongs here if it forwards the media port too. Empty derives them from the listener",
            "bind address, or from this host's addresses and what stun_discovery finds."
    })
    @Setter
    private List<String> advertiseAddresses = new ArrayList<>();

    @Path("stun_discovery")
    @Comments({
            "Finds this proxy's public address through STUN when advertise_addresses is empty. Built-in",
            "signaling asks public STUN servers from udp_port, NXS the signaling service's own"
    })
    @Setter
    private boolean stunDiscovery = true;

    @Path("udp_port")
    @Comments({
            "Dedicated UDP port for NetherNet media, one socket for every peer, which STUN discovery",
            "shares. It must not be the listener port, which RakNet already holds. 0 picks the first port",
            "after the listener port that no server in servers uses, which the startup log names."
    })
    private int udpPort = 0;

    @Path("https")
    @Comment("Serves signaling over HTTPS as well as HTTP, on the same port")
    private HttpsSettings https = new HttpsSettings();

    @Path("identity_file")
    @Comments({
            "Unencrypted PEM private key holding the P-384 key identifying this operator to",
            "clients, generated on first start. Share it across a fleet to be trusted as one",
            "operator; replacing it re-prompts every returning player. An absolute path works,",
            "so it can be mounted from outside the proxy directory."
    })
    private String identityFile = "keys/identity.pem";

    /**
     * The port media binds for a listener: udp_port, or for 0 the first port after the listener's
     * that no server uses, as a server on this machine would already hold it.
     *
     * @param serverPorts The ports of the servers in the server list
     */
    public int resolveUdpPort(int listenerPort, Set<Integer> serverPorts) {
        if (this.udpPort > 0) {
            return this.udpPort;
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

    public SignalingMode signalingMode() {
        try {
            return SignalingMode.valueOf(this.signalingMode.toUpperCase());
        } catch (IllegalArgumentException e) {
            return SignalingMode.BUILTIN;
        }
    }
}
