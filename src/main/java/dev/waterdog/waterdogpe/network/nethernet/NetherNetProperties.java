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

package dev.waterdog.waterdogpe.network.nethernet;

import dev.waterdog.waterdogpe.utils.config.proxy.NetherNetSettings;
import lombok.extern.log4j.Log4j2;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * NetherNet tuning that lives in system properties rather than the config, since almost no install
 * changes it, and overrides of config options for a host that cannot edit the config. Read once at
 * startup.
 * <ul>
 * <li>{@code waterdog.nethernetIceServers}: STUN and TURN servers ICE may use, comma separated, such
 * as {@code stun:stun.l.google.com:19302}. A proxy behind NAT needs STUN to learn its public
 * address, TURN relays when no direct path exists. TURN credentials go in the URL, as
 * {@code turn:user:password@host:3478}.</li>
 * <li>{@code waterdog.nethernetHandshakeTimeout}: seconds a connection has to finish ICE and DTLS
 * before it is dropped. Defaults to 30.</li>
 * <li>{@code waterdog.nethernetTransportMemory}: seconds a {@code server_type: bedrock} downstream
 * remembers which transport worked, so not every join probes. 0 probes every join. Defaults to
 * 300.</li>
 * <li>{@code waterdog.nethernetSignalingPort}: TCP port for the signaling endpoint when it cannot
 * share the listener port, which is where clients look for it. 0 mirrors the listener port.</li>
 * <li>{@code waterdog.nethernetMaxConnections}: concurrent NetherNet connections, 0 for no
 * limit.</li>
 * <li>{@code waterdog.nethernetLog}: the level NetherNet logs at, the native ICE and DTLS stack and
 * NetherNet's own logs alike: OFF, ERROR, WARN, INFO, DEBUG or TRACE. DEBUG also shows what
 * signaling and STUN discovery do. Unset, the native stack logs at WARN.</li>
 * <li>{@code waterdog.nethernetStunServers}: the servers built-in signaling asks for this proxy's
 * public address, comma separated {@code stun:host[:port]} URLs, empty for none. Defaults to
 * Cloudflare's and Google's, as either may be blocked where a proxy runs. Unlike
 * {@code waterdog.nethernetIceServers}, they are asked once in the background, not by every
 * peer.</li>
 * <li>{@code waterdog.nethernetAdvertiseAddresses}: overrides {@code nethernet.advertise_addresses},
 * comma separated. Empty keeps the config's.</li>
 * <li>{@code waterdog.nethernetStunDiscovery}: overrides {@code nethernet.stun_discovery}, true or
 * false.</li>
 * <li>{@code waterdog.nethernetSignalingMode}: overrides {@code nethernet.signaling_mode}, builtin,
 * nxs, hybrid or plugin.</li>
 * </ul>
 */
@Log4j2
public final class NetherNetProperties {

    public static final List<String> ICE_SERVERS = list(System.getProperty("waterdog.nethernetIceServers", ""));
    public static final int HANDSHAKE_TIMEOUT = Integer.getInteger("waterdog.nethernetHandshakeTimeout", 30);
    public static final int TRANSPORT_MEMORY = Integer.getInteger("waterdog.nethernetTransportMemory", 300);
    public static final int SIGNALING_PORT = Integer.getInteger("waterdog.nethernetSignalingPort", 0);
    public static final int MAX_CONNECTIONS = Integer.getInteger("waterdog.nethernetMaxConnections", 0);
    /** Null when the property is not set. */
    public static final String LOG_LEVEL = System.getProperty("waterdog.nethernetLog");
    /** Null when the property is not set, so the defaults apply. */
    public static final List<String> STUN_SERVERS = optionalList(System.getProperty("waterdog.nethernetStunServers"));
    public static final List<String> DEFAULT_STUN_SERVERS =
            List.of("stun:stun.cloudflare.com:3478", "stun:stun.l.google.com:19302");
    private static final List<String> ADVERTISE_ADDRESSES =
            list(System.getProperty("waterdog.nethernetAdvertiseAddresses", ""));
    private static final String STUN_DISCOVERY = System.getProperty("waterdog.nethernetStunDiscovery");
    private static final String SIGNALING_MODE = System.getProperty("waterdog.nethernetSignalingMode");

    private NetherNetProperties() {
    }

    /**
     * Applies the config overrides to a freshly loaded config, in memory only, so the file keeps
     * what it says. A value that is not one keeps the config's, with an error saying so.
     */
    public static void applyOverrides(NetherNetSettings settings) {
        if (!ADVERTISE_ADDRESSES.isEmpty()) {
            settings.setAdvertiseAddresses(ADVERTISE_ADDRESSES);
            log.info("NetherNet advertise addresses set from system property: {}", String.join(", ", ADVERTISE_ADDRESSES));
        }
        if (STUN_DISCOVERY != null) {
            if (STUN_DISCOVERY.equalsIgnoreCase("true") || STUN_DISCOVERY.equalsIgnoreCase("false")) {
                settings.setStunDiscovery(Boolean.parseBoolean(STUN_DISCOVERY));
                log.info("NetherNet STUN discovery set from system property: {}", settings.isStunDiscovery());
            } else {
                log.error("Invalid waterdog.nethernetStunDiscovery \"{}\", expected true or false, keeping the configured value", STUN_DISCOVERY);
            }
        }
        if (SIGNALING_MODE != null) {
            try {
                NetherNetSettings.SignalingMode mode = NetherNetSettings.SignalingMode.valueOf(SIGNALING_MODE.trim().toUpperCase(Locale.ROOT));
                settings.setSignalingMode(mode);
                log.info("NetherNet signaling mode set from system property: {}", mode.name().toLowerCase(Locale.ROOT));
            } catch (IllegalArgumentException invalid) {
                log.error("Invalid waterdog.nethernetSignalingMode \"{}\", expected builtin, nxs, hybrid or plugin, keeping the configured mode", SIGNALING_MODE);
            }
        }
    }

    private static List<String> optionalList(String value) {
        return value == null ? null : list(value);
    }

    private static List<String> list(String value) {
        return Arrays.stream(value.split(",")).map(String::trim).filter(entry -> !entry.isEmpty()).toList();
    }
}
