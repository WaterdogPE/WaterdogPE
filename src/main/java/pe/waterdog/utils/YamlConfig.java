/**
 * Copyright 2020 WaterdogTEAM
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pe.waterdog.utils;

import com.google.common.base.Charsets;
import org.yaml.snakeyaml.Yaml;
import pe.waterdog.logger.MainLogger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class YamlConfig extends Configuration {

    private final static Yaml yaml = new Yaml();

    public YamlConfig(File file) {
        super(file);
    }

    public YamlConfig(Path path) {
        super(path);
    }

    public YamlConfig(String file) {
        super(file);
    }

    @Override
    public void load() {
        try {
            this.values = yaml.loadAs(Files.newInputStream(this.file.toPath()), Map.class);
        } catch (Exception e) {
            MainLogger.getLogger().error("Unable to load Config " + this.file.toString());
        }
    }

    @Override
    public void save() {
        String writingData = yaml.dump(this.values);
        try {
            Files.write(this.file.toPath(), writingData.getBytes(Charsets.UTF_8));
        } catch (IOException e) {
            MainLogger.getLogger().error("Unable to save Config " + this.file.toString());
        }
    }
}
