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
final class ConfigMigrations {

    private static final List<Consumer<ConfigSection>> STEPS = List.of(
            ConfigMigrations::shareNetherNetAddresses
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
     * Step 1: built-in signaling and NXS share one advertise_addresses list, each address once, and the
     * NXS stun_discovery switch moves next to it.
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
}
