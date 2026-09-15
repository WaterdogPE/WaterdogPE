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
import lombok.experimental.Accessors;
import net.cubespace.Yamler.Config.Comment;
import net.cubespace.Yamler.Config.Comments;
import net.cubespace.Yamler.Config.Path;

import java.util.ArrayList;
import java.util.List;

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
    private String signalingMode = "builtin";

    @Path("nxs")
    @Comment("Settings for the NXS signaling provider. Only used in the nxs and hybrid modes")
    private NxsSettings nxs = new NxsSettings();

    @Path("signaling_port")
    @Comment("TCP port for the signaling endpoint. 0 mirrors the listener port, which is what clients expect")
    private int signalingPort = 0;

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
            "Addresses put in the ICE candidates clients connect to. Empty derives them from the",
            "listener bind address. Set it when clients reach a different address or the listener",
            "binds a wildcard. An address this machine does not hold is announced as the public side",
            "of a NAT forwarding udp_port here, same port. Media bypasses a signaling reverse proxy,",
            "so its address only belongs here if it forwards the media port too."
    })
    private List<String> advertiseAddresses = new ArrayList<>();

    @Path("ice_servers")
    @Comments({
            "STUN and TURN servers ICE may use, such as stun:stun.l.google.com:19302.",
            "A proxy behind NAT needs STUN to learn its public address, TURN relays when no direct",
            "path exists. TURN credentials go in the URL, as turn:user:password@host:3478."
    })
    private List<String> iceServers = new ArrayList<>();

    @Path("udp_port")
    @Comments({
            "Dedicated UDP port for NetherNet media, one socket for every peer.",
            "It must not be the listener port, which RakNet already holds.",
            "0 picks an ephemeral port per peer, which needs no fixed port open but publishes a",
            "different one to every client."
    })
    private int udpPort = 0;

    @Path("https")
    @Comment("Serves signaling over HTTPS as well as HTTP, on the same port")
    private HttpsSettings https = new HttpsSettings();

    @Path("identity_file")
    @Comments({
            "Unencrypted PEM private key holding the P-384 key identifying this operator to",
            "clients, generated on first start. Share it across a fleet to be trusted as one",
            "operator; replacing it re-prompts every returning player."
    })
    private String identityFile = "keys/identity.pem";

    @Path("identity_domain")
    @Comments({
            "Operator name shown in the first use trust prompt. Empty falls back to listener.name.",
            "Clients pin the key, not the name, so changing it prompts nobody again."
    })
    private String identityDomain = "";

    @Path("handshake_timeout")
    @Comment("Seconds a connection has to finish ICE and DTLS before it is dropped")
    private int handshakeTimeout = 30;

    @Path("transport_memory")
    @Comments({
            "Seconds a server_type: bedrock downstream remembers which transport worked, so not every",
            "join probes. A failure on the remembered transport switches at once. 0 probes every join."
    })
    private int transportMemory = 300;

    @Path("max_connections")
    @Comment("Maximum concurrent NetherNet connections. 0 follows the global player limit")
    private int maxConnections = 0;

    public SignalingMode signalingMode() {
        try {
            return SignalingMode.valueOf(this.signalingMode.toUpperCase());
        } catch (IllegalArgumentException e) {
            return SignalingMode.BUILTIN;
        }
    }
}
