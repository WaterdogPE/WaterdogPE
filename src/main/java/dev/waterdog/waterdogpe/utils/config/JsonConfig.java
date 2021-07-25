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

import com.google.gson.Gson;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;

public class JsonConfig extends Configuration {

    protected static Gson json = new Gson();

    public JsonConfig(File file) {
        super(file);
    }

    public JsonConfig(Path path) {
        super(path);
    }

    public JsonConfig(String file) {
        super(file);
    }

    public JsonConfig(File saveFile, InputStream inputStream) {
        super(saveFile, inputStream);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Map<String, Object> deserialize(InputStream inputStream) {
        return json.fromJson(new InputStreamReader(inputStream, StandardCharsets.UTF_8), Map.class);
    }

    @Override
    protected String serialize(Map<String, Object> values) {
        return json.toJson(this.values);
    }
}