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

import java.util.List;
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

    private static final List<Consumer<ConfigSection>> STEPS = List.of();

    static final int CURRENT = STEPS.size();

    private ConfigMigrations() {
    }

    static void migrate(ConfigSection root, int version) {
        for (int step = version; step < CURRENT; step++) {
            STEPS.get(step).accept(root);
        }
    }
}
