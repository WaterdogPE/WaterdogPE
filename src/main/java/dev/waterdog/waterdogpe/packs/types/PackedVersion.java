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

package dev.waterdog.waterdogpe.packs.types;

import com.google.gson.*;
import dev.waterdog.waterdogpe.utils.FileUtils;

import java.lang.reflect.Type;

public class PackedVersion {

    private final int[] version;

    public PackedVersion(int major, int minor, int patch) {
        this.version = new int[]{major, minor, patch};
    }

    @Override
    public String toString() {
        return String.format("%d.%d.%d", this.version[0], this.version[1], this.version[2]);
    }

    public static class Serializer implements JsonSerializer<PackedVersion> {

        @Override
        public JsonElement serialize(PackedVersion version, Type typeOfSrc, JsonSerializationContext context) {
            return FileUtils.GSON.toJsonTree(version.version);
        }
    }

    public static class Deserializer implements JsonDeserializer<PackedVersion> {

        @Override
        public PackedVersion deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            int[] version = context.deserialize(json, int[].class);
            return new PackedVersion(version[0], version[1], version[2]);
        }
    }
}
