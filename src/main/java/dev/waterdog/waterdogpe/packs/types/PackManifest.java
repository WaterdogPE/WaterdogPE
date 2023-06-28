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

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import dev.waterdog.waterdogpe.utils.FileUtils;
import lombok.Data;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Data
public class PackManifest {

    public static final Path MANIFEST_PATH = Paths.get("manifest.json");

    public static final String BEHAVIOR_PACK = "behavior";
    public static final String RESOURCE_PACK = "resources";
    @SerializedName("format_version")
    private String formatVersion;
    private ManifestHeader header;
    private List<PackModule> modules = Collections.emptyList();
    private JsonElement metadata;
    private List<JsonElement> dependencies = Collections.emptyList();
    private List<String> capabilities = Collections.emptyList();
    @SerializedName("subpacks")
    private List<JsonElement> subPacks = Collections.emptyList();

    public static PackManifest fromStream(InputStream stream) {
        return FileUtils.GSON.fromJson(new InputStreamReader(stream), PackManifest.class);
    }

    public boolean validate() {
        if (this.formatVersion == null || this.header == null || this.modules == null) {
            return false;
        }
        return this.header.description != null && this.header.name != null && this.header.uuid != null && this.header.version != null;
    }

    @Data
    public static class ManifestHeader {
        private String name;
        private UUID uuid;
        private String description;

        private PackedVersion version;
        @SerializedName("platform_locked")
        private boolean platformLocked;
        @SerializedName("min_engine_version")
        private PackedVersion minEngineVersion;

        @SerializedName("pack_scope")
        private String packScope = "global";
        @SerializedName("directory_load")
        private boolean directoryLoad;
        @SerializedName("load_before_game")
        private boolean loadBeforeGame;
        @SerializedName("lock_template_options")
        private boolean lockTemplateOptions;
        @SerializedName("population_control")
        private boolean populationControl;
    }

    @Data
    public static class PackModule {
        private UUID uuid;
        private String description;
        private PackedVersion version;
        private String type;
    }
}
