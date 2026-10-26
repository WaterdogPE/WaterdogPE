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

import lombok.extern.log4j.Log4j2;
import net.cubespace.Yamler.Config.ConfigSection;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Brings a config.yml written in an older layout up to date, on the raw file before any option is
 * read. The layout version is the number of steps applied, kept in config_version.
 * <p>
 * A step is needed when a pushed layout renames, moves or removes an option. Adding an option needs
 * none, since the loader writes new options back by itself and an older proxy still reads the file.
 * Changed defaults are left alone, as the file holds every option and an old default cannot be told
 * apart from a choice.
 * <p>
 * Note {@link ConfigSection#get} and {@link ConfigSection#set} create every section on the way, so a
 * path is checked with {@link ConfigSection#has} first.
 */
@Log4j2
final class ConfigMigrations {

    private static final List<Consumer<ConfigSection>> STEPS = List.of(
            ConfigMigrations::netherNetLayout,
            ConfigMigrations::proxyProtocolWhitelist
    );

    static final int CURRENT = STEPS.size();

    private ConfigMigrations() {
    }

    static void migrate(ConfigSection root, int version) {
        for (int step = version; step < CURRENT; step++) {
            STEPS.get(step).accept(root);
        }
    }

    /**
     * Step 1: the NetherNet options take Geyser's names and its signaling block, and
     * nethernet.enabled becomes listener.transport.
     */
    private static void netherNetLayout(ConfigSection root) {
        String enabled = "nethernet.enabled";
        if (root.has(enabled)) {
            // RakNet always ran, with NetherNet next to it or not
            if (!root.has("listener.transport")) {
                root.set("listener.transport", isTrue(root, enabled) ? "both" : "raknet");
            }
            root.remove(enabled);
        }
        // Merged first, as the shared list then moves into the signaling block
        shareNetherNetAddresses(root);
        move(root, "nethernet.udp_port", "nethernet.webrtc_port");
        move(root, "nethernet.signaling_mode", "nethernet.signaling.mode");
        for (String key : List.of("advertise_addresses", "stun_discovery", "trusted_proxies", "proxy_protocol", "nxs")) {
            move(root, "nethernet." + key, "nethernet.signaling." + key);
        }
        move(root, "nethernet.https", "nethernet.signaling.builtin.https");
    }

    /**
     * Step 2: nethernet.signaling.trusted_proxies is X-Forwarded-For only, and PROXY headers are read from
     * network_settings.proxy_protocol_whitelisted_ips, as in Geyser. With proxy_protocol on, the trusted
     * proxies were read for both, so they go in both lists. The whitelist also routes NetherNet gameplay
     * traffic through its proxies, which the old layout never did, so the operator is told.
     */
    private static void proxyProtocolWhitelist(ConfigSection root) {
        String switchPath = "nethernet.signaling.proxy_protocol";
        if (!root.has(switchPath)) {
            return;
        }
        boolean proxyProtocol = isTrue(root, switchPath);
        root.remove(switchPath);
        if (!proxyProtocol) {
            return;
        }

        String whitelistPath = "network_settings.proxy_protocol_whitelisted_ips";
        Set<String> whitelist = new LinkedHashSet<>();
        for (String path : List.of(whitelistPath, "nethernet.signaling.trusted_proxies")) {
            if (root.has(path) && root.get(path) instanceof List<?> entries) {
                entries.forEach(entry -> whitelist.add(String.valueOf(entry).trim()));
            }
        }
        whitelist.remove("");
        root.set(whitelistPath, new ArrayList<>(whitelist));

        String enabledPath = "network_settings.enable_proxy_protocol";
        if (!root.has(enabledPath) || !isTrue(root, enabledPath)) {
            log.warn("nethernet.signaling.proxy_protocol is gone, so signaling connections that carry a PROXY header "
                    + "fail until network_settings.enable_proxy_protocol is on, which makes RakNet require them and "
                    + "routes NetherNet gameplay traffic through network_settings.proxy_protocol_whitelisted_ips too");
        } else if (!whitelist.isEmpty()) {
            log.warn("RakNet and NetherNet gameplay traffic now only come through "
                    + "network_settings.proxy_protocol_whitelisted_ips, which this update filled from "
                    + "nethernet.signaling.trusted_proxies: add RakNet's proxies if they differ, forward webrtc_port "
                    + "through them with PROXY v2 headers, and name them in nethernet.signaling.advertise_addresses, "
                    + "without which NetherNet does not start");
        }
    }

    /**
     * Built-in signaling and NXS share one advertise_addresses list, each address once, and the NXS
     * stun_discovery switch moves next to it.
     */
    private static void shareNetherNetAddresses(ConfigSection root) {
        String nxsAddresses = "nethernet.nxs.advertise_addresses";
        if (root.has(nxsAddresses)) {
            Set<String> merged = new LinkedHashSet<>();
            for (String path : List.of("nethernet.advertise_addresses", nxsAddresses)) {
                if (root.get(path) instanceof List<?> addresses) {
                    addresses.forEach(address -> merged.add(String.valueOf(address).trim()));
                }
            }
            merged.remove("");
            root.set("nethernet.advertise_addresses", new ArrayList<>(merged));
            root.remove(nxsAddresses);
        }

        String nxsStunDiscovery = "nethernet.nxs.advanced.stun_discovery";
        if (root.has(nxsStunDiscovery)) {
            if (!root.has("nethernet.stun_discovery")) {
                root.set("nethernet.stun_discovery", root.get(nxsStunDiscovery));
            }
            root.remove(nxsStunDiscovery);
        }
    }

    private static boolean isTrue(ConfigSection root, String path) {
        Object value = root.get(path);
        return Boolean.parseBoolean(String.valueOf(value));
    }

    /**
     * Moves an option, keeping the one already at the new path if the file has both.
     */
    private static void move(ConfigSection root, String from, String to) {
        if (!root.has(from)) {
            return;
        }
        if (!root.has(to)) {
            root.set(to, root.get(from));
        }
        root.remove(from);
    }
}
