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

package dev.waterdog.utils;

import dev.waterdog.logger.MainLogger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public abstract class Configuration {

    protected File file;
    protected Map<String, Object> values = new LinkedHashMap<>();

    public Configuration(String saveFile) {
        this(new File(saveFile));
    }

    public Configuration(Path path) {
        this(path.toFile());
    }

    public Configuration(File saveFile) {
        this(saveFile, null);
    }

    public Configuration(File saveFile, InputStream inputStream) {
        this.file = saveFile;

        try {
            if (!this.file.exists()) {
                this.file.getParentFile().mkdirs();
                this.file.createNewFile();
            }

            if (inputStream == null) {
                inputStream = Files.newInputStream(this.file.toPath());
            }
            this.load(inputStream);
        } catch (IOException e) {
            MainLogger.getLogger().error("Unable to create Config " + this.file.toString(), e);
        }
    }

    public abstract void load(InputStream inputStream);

    public abstract void save();

    public void loadFrom(Map<String, Object> values) {
        this.values = values;
    }

    public Set<String> getKeys() {
        return new HashSet<>(this.values.keySet());
    }

    public void set(String key, Object value) {
        this.values.put(key, value);
    }

    public Object get(String key) {
        return this.get(key, null);
    }

    public Object get(String key, Object defaultValue) {
        if (key == null || key.isEmpty()) return defaultValue;
        String[] keys = key.split("\\.");

        if (!this.values.containsKey(keys[0])) return defaultValue;

        Object value = this.values.get(keys[0]);
        if (!(value instanceof Map) || keys.length == 1) return value == null ? defaultValue : value;

        for (int i = 1; i < keys.length; i++) {
            value = ((Map<?, ?>) value).get(keys[i]);
            if (!(value instanceof Map)) {
                return value == null ? defaultValue : value;
            }
        }

        return value;
    }


    public void setString(String key, String value) {
        this.values.put(key, value);
    }

    public String getString(String key) {
        return this.getString(key, null);
    }

    public String getString(String key, String defaultValue) {
        return (String) this.get(key, defaultValue);
    }


    public void setInt(String key, Integer value) {
        this.values.put(key, value);
    }

    public Integer getInt(String key) {
        return this.getInt(key, null);
    }

    public Integer getInt(String key, Integer defaultValue) {
        return (Integer) this.get(key, defaultValue);
    }


    public void setLong(String key, Long value) {
        this.values.put(key, value);
    }

    public Long getLong(String key) {
        return this.getLong(key, null);
    }

    public Long getLong(String key, Long defaultValue) {
        return (Long) this.get(key, defaultValue);
    }


    public void setDouble(String key, Double value) {
        this.values.put(key, value);
    }

    public Double getDouble(String key) {
        return this.getDouble(key, null);
    }

    public Double getDouble(String key, Double defaultValue) {
        return (Double) this.get(key, defaultValue);
    }


    public void setBoolean(String key, Boolean value) {
        this.values.put(key, value);
    }

    public Boolean getBoolean(String key) {
        return getBoolean(key, null);
    }

    public Boolean getBoolean(String key, Boolean defaultValue) {
        return (Boolean) this.get(key, defaultValue);
    }


    public <T> void setList(String key, List<T> value) {
        this.values.put(key, value);
    }

    public <T> List<T> getList(String key) {
        return this.getList(key, null);
    }

    public <T> List<T> getList(String key, List<T> defaultValue) {
        return (List<T>) this.get(key, defaultValue);
    }


    public void setStringList(String key, List<String> value) {
        this.values.put(key, value);
    }

    public List<String> getStringList(String key) {
        return getStringList(key, null);
    }

    public List<String> getStringList(String key, List<String> defaultValue) {
        return (List<String>) this.get(key, defaultValue);
    }
}
