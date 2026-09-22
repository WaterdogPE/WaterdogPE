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

import java.util.Arrays;
import java.util.List;

/**
 * NetherNet tuning that lives in system properties rather than the config, since almost no install
 * changes it. Read once at startup.
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
 * <li>{@code waterdog.nethernetLog}: the level the native ICE and DTLS stack logs at. Defaults to
 * WARN.</li>
 * </ul>
 */
public final class NetherNetProperties {

    public static final List<String> ICE_SERVERS = list(System.getProperty("waterdog.nethernetIceServers", ""));
    public static final int HANDSHAKE_TIMEOUT = Integer.getInteger("waterdog.nethernetHandshakeTimeout", 30);
    public static final int TRANSPORT_MEMORY = Integer.getInteger("waterdog.nethernetTransportMemory", 300);
    public static final int SIGNALING_PORT = Integer.getInteger("waterdog.nethernetSignalingPort", 0);
    public static final int MAX_CONNECTIONS = Integer.getInteger("waterdog.nethernetMaxConnections", 0);
    public static final String NATIVE_LOG_LEVEL = System.getProperty("waterdog.nethernetLog", "WARN");

    private NetherNetProperties() {
    }

    private static List<String> list(String value) {
        return Arrays.stream(value.split(",")).map(String::trim).filter(entry -> !entry.isEmpty()).toList();
    }
}
