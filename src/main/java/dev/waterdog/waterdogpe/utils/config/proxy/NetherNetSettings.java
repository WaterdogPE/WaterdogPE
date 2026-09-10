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
         * Nothing is bound. Offers arrive through the signaling API, driven by a plugin.
         */
        EXTERNAL
    }

    @Path("enabled")
    @Accessors(fluent = true)
    @Comment("Accept NetherNet connections alongside RakNet")
    private boolean enabled = false;

    @Path("signaling_mode")
    @Comments({
            "builtin: the proxy serves the HTTP signaling endpoint on the listener port",
            "external: nothing is bound, a plugin feeds offers through the signaling API"
    })
    private String signalingMode = "builtin";

    @Path("signaling_port")
    @Comment("TCP port for the signaling endpoint. 0 mirrors the listener port, which is what clients expect")
    private int signalingPort = 0;

    @Path("trusted_proxies")
    @Comments({
            "Addresses allowed to set X-Forwarded-For on signaling requests, as single hosts or",
            "CIDR ranges such as 10.0.0.0/8 or 2001:db8::/32. An http or https entry is fetched",
            "and read as one address per line.",
            "Mojang recommends fronting the signaling port with a reverse proxy, this is how the",
            "real client address survives that hop."
    })
    private List<String> trustedProxies = new ArrayList<>();

    @Path("proxy_protocol")
    @Comments({
            "Read a HAProxy PROXY header, v1 or v2, from signaling connections that arrive from a",
            "trusted_proxies address. A connection without a header is still served, so one listener",
            "takes both. Without a header, X-Forwarded-For is used instead."
    })
    private boolean proxyProtocol = false;

    @Path("advertise_addresses")
    @Comments({
            "Addresses to put in the ICE candidates clients connect to.",
            "Empty derives them from the listener bind address, which is usually what you want: a",
            "proxy bound to one public address should not be offering clients the internal addresses",
            "of the machine it runs on. Set this when the address clients reach differs from the",
            "bound one, or when the listener binds a wildcard."
    })
    private List<String> advertiseAddresses = new ArrayList<>();

    @Path("udp_port")
    @Comments({
            "Dedicated UDP port for NetherNet media, multiplexing every peer over one socket.",
            "It must not be the listener port, which RakNet already holds.",
            "0 lets the operating system pick an ephemeral port per peer, which works without",
            "opening a fixed port but publishes a different one to every client."
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
            "The operator name players see in the first use trust prompt, so set it to something",
            "they will recognise and keep it stable. Empty falls back to listener.name.",
            "Changing it does not prompt anyone again, because clients pin the key and not the name."
    })
    private String identityDomain = "";

    @Path("handshake_timeout")
    @Comment("Seconds a connection has to finish ICE and DTLS before it is dropped")
    private int handshakeTimeout = 30;


    @Path("transport_memory")
    @Comments({
            "How long a server_type: bedrock downstream remembers which transport worked, in seconds.",
            "Keeps every join from paying for a probe without pinning the answer: the memory",
            "expires, and a failure on the remembered transport switches to the other one at once.",
            "0 probes on every connection."
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
