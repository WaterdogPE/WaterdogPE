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

package dev.waterdog.waterdogpe.utils.config.proxy;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.cubespace.Yamler.Config.Comment;
import net.cubespace.Yamler.Config.Comments;
import net.cubespace.Yamler.Config.Path;
import org.cloudburstmc.netty.channel.raknet.RakConstants;

import java.util.ArrayList;
import java.util.List;

@Getter
public class NetworkSettings extends SettingsSection {

    @Path("enable_ipv6")
    @Accessors(fluent = true)
    @Comment("If enabled, the proxy will be able to bind to an Ipv6 Address")
    private boolean enableIpv6 = false;

    @Path("max_user_mtu")
    @Comment("Maximum MTU size of user <-> proxy connection that is allowed")
    private int maximumMtu = RakConstants.MAXIMUM_MTU_SIZE;

    @Path("max_downstream_mtu")
    @Comment("Maximum MTU size of proxy <-> server connection that is allowed")
    private int maximumDownstreamMtu = RakConstants.MAXIMUM_MTU_SIZE;

    @Path("connection_timeout")
    @Comment("Server connection timeout in seconds")
    private int connectTimeout = 15;

    @Comment("Connection throttle time in milliseconds")
    @Path("connection_throttle_time")
    private int connectionThrottleTime = 1000;

    @Comment("Number of connections that can be opened in \"connection_throttle_time\" interval from the same address. To disable set value to -1")
    @Path("connection_throttle")
    private int connectionThrottle = 5;

    @Comment("Number of login requests that can be made in \"connection_throttle_time\" interval from the same address. To disable set value to -1")
    @Path("login_throttle")
    private int loginThrottle = 2;

    @Path("enable_cookies")
    @Accessors(fluent = true)
    @Comment("Enable RakNet cookies for additional security. Do NOT disable this unless you know what you are doing.")
    private boolean enableCookies = true;

    @Path("proxy_protocol_whitelisted_ips")
    @Comments({
            "The HAProxy PROXY protocol (v1/v2) speaking proxies in front of this proxy, as hosts or CIDR ranges such as 10.0.0.0/8.",
            "Listing one turns PROXY protocol on, so leave this empty unless you run such a proxy.",
            "RakNet: only these proxies may connect, and each connection has to start with a PROXY header.",
            "NetherNet signaling: PROXY headers are read from these proxies. Their connections without one are still accepted.",
            "Proxies in this list are also not affected by built-in signaling's per-address connection limit.",
            "An http or https entry is fetched and read as one address per line.",
            "IF YOU DON'T KNOW WHAT THIS IS, DON'T TOUCH IT!"
    })
    private List<String> proxyProtocolWhitelistedIps = new ArrayList<>();

    @Path("webrtc_proxy_protocol_relays")
    @Comments({
            "UDP relays in front of nethernet.webrtc_port that send a PROXY protocol v2 header with each player's address,",
            "as hosts or CIDR ranges. Leave this empty unless you run such a relay.",
            "While set, players can only reach webrtc_port through these relays, so nethernet.signaling.advertise_addresses",
            "has to name the relays, or NetherNet does not start.",
            "This is separate from proxy_protocol_whitelisted_ips and nethernet.signaling.trusted_proxies, which only cover",
            "RakNet and signaling. An http or https entry is fetched and read as one address per line.",
            "IF YOU DON'T KNOW WHAT THIS IS, DON'T TOUCH IT!"
    })
    private List<String> webrtcProxyProtocolRelays = new ArrayList<>();

    @Path("error_timeout")
    @Accessors(fluent = true)
    @Comment("How many seconds should a connection be blocked if an error is thrown. Set to 0 to disable.")
    private int errorTimeout = 15;

    @Path("login_timeout")
    @Accessors(fluent = true)
    @Comment("How many seconds a connection has to complete the login handshake (network settings -> login -> client-to-server handshake) before it is disconnected. Set to 0 to disable.")
    private int loginTimeout = 10;

    @Path("random_downstream_loopback_address")
    @Accessors(fluent = true)
    @Comment("Bind each downstream connection to a random 127.0.0.0/8 source address when the target server is on loopback. Certain server softwares like BDS rate limit connections per IP, this causes problems when transferring multiple players at the same time to a certain server. Requires an OS that allows binding the whole loopback range.")
    private boolean randomDownstreamLoopbackAddress = false;

    /** Whether any proxy is listed, parsed or not, which turns PROXY protocol on, so a list of typos or unreachable URLs fails closed. */
    public boolean proxyProtocol() {
        return this.proxyProtocolWhitelistedIps != null
                && this.proxyProtocolWhitelistedIps.stream().anyMatch(entry -> entry != null && !entry.isBlank());
    }
}
