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

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.UUID;

public abstract class ResourcePack {

    public static final String TYPE_RESOURCES = "resources";
    public static final String TYPE_DATA = "data";

    protected final Path packPath;
    protected PackManifest packManifest;
    protected String contentKey;

    public ResourcePack(Path packPath) {
        this.packPath = packPath;
    }

    public abstract long getPackSize();

    public abstract byte[] getHash();

    public abstract byte[] getChunk(int off, int len);

    public abstract void saveToCache() throws IOException;

    public abstract ByteBuffer getCachedPack();

    public abstract InputStream getStream(Path path) throws IOException;

    public boolean loadManifest() throws IOException {
        try (InputStream stream = this.getStream(PackManifest.MANIFEST_PATH)) {
            if (stream != null) {
                this.packManifest = PackManifest.fromStream(stream);
                return true;
            }
        }
        return false;
    }

    public PackManifest getPackManifest() {
        return this.packManifest;
    }

    public String getPackName() {
        return this.packManifest.getHeader().getName();
    }

    public UUID getPackId() {
        return this.packManifest.getHeader().getUuid();
    }

    public PackedVersion getVersion() {
        return this.packManifest.getHeader().getVersion();
    }

    public String getType() {
        return this.packManifest.getModules().get(0).getType();
    }

    public Path getPackPath() {
        return this.packPath;
    }

    public String getContentKey() {
        return contentKey;
    }

    public void setContentKey(String contentKey) {
        this.contentKey = contentKey;
    }
}
