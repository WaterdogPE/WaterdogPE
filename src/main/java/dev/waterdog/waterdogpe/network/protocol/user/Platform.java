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

package dev.waterdog.waterdogpe.network.protocol.user;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public enum Platform {
    UNKNOWN("Unknown", -1),
    ANDROID("Android", 1),
    IOS("iOS", 2),
    MAC_OS("macOS", 3),
    FIRE_OS("FireOS", 4),
    GEAR_VR("Gear VR", 5),
    HOLOLENS("Hololens", 6),
    WINDOWS_10("Windows 10", 7),
    WINDOWS("Windows", 8),
    DEDICATED("Dedicated", 9),
    TVOS("TVOS", 10),
    PLAYSTATION("PlayStation", 11),
    SWITCH("Switch", 12),
    XBOX_ONE("Xbox One", 13),
    WINDOWS_PHONE("Windows Phone", 14),
    LINUX("Linux", 15);

    private static Map<Integer, Platform> PLATFORM_BY_ID = new HashMap<>();

    static {
        for (Platform platform : Platform.values()) {
            PLATFORM_BY_ID.put(platform.getId(), platform);
        }
    }

    public static Platform getPlatformByID(int id) {
        if (PLATFORM_BY_ID.containsKey(id)) {
            return PLATFORM_BY_ID.get(id);
        }

        return Platform.UNKNOWN;
    }

    public static Platform[] VALUES = values();

    @Getter
    private final String name;

    @Getter
    private final int id;

    Platform(String name, int id) {
        this.name = name;
        this.id = id;
    }


}
