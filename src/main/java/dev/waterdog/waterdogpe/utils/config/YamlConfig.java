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

package dev.waterdog.waterdogpe.utils.config;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;

public class YamlConfig extends Configuration {

    private final static Yaml yaml;

    static {
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumperOptions.setPrettyFlow(true);
        yaml = new Yaml(dumperOptions);
    }

    public YamlConfig(String file) {
        super(file);
    }

    public YamlConfig(Path path) {
        super(path);
    }

    public YamlConfig(File saveFile) {
        super(saveFile);
    }

    public YamlConfig(File saveFile, InputStream inputStream) {
        super(saveFile, inputStream);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Map<String, Object> deserialize(InputStream inputStream) {
        return yaml.loadAs(inputStream, Map.class);
    }

    @Override
    protected String serialize(Map<String, Object> values) {
        return yaml.dump(values);
    }
}