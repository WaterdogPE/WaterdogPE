/*
 * Copyright 2021 WaterdogTEAM
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

package dev.waterdog.waterdogpe.utils.config;

import net.cubespace.Yamler.Config.YamlConfig;
import net.cubespace.Yamler.Config.*;

import java.io.File;
import java.util.*;

@SerializeOptions(skipFailedObjects = true)
public class CredentialsConfig extends YamlConfig
{
    public CredentialsConfig(File file) {
        this.CONFIG_HEADER = new String[]{"Waterdog Credentials Configuration file", "Configure different credentials or confidential keys here. Separated from the config.yml to avoid leaks."};
        this.CONFIG_FILE = file;
    }

    @Path("texture_keys")
    @Comment("When using encrypted texture packs, put the content keys here for them to be sent automatically. This is a Map<PackUUID, ContentKey>")
    private HashMap<String, String> textureKeys = new HashMap<>();

    public Map<String, String> getTextureKeys(){
        return this.textureKeys;
    }
}