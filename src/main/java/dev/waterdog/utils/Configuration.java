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
import lombok.AllArgsConstructor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;

public abstract class Configuration {

    @AllArgsConstructor
    private static class LastMap {
        public final String key;
        public final Map<String, Object> map;
    }

    protected File file;
    protected Map<String, Object> values = new LinkedHashMap<>();

    public Configuration() {
        this((File) null);
    }

    public Configuration(String file) {
        this(new File(file));
    }

    public Configuration(Path path) {
        this(path.toFile());
    }

    public Configuration(File file) {
        this.file = file;

        if (this.file != null && !this.file.exists()) {
            try {
                File parentFile = this.file.getParentFile();

                if (parentFile != null) {
                    parentFile.mkdirs();
                }

                FileWriter myWriter = new FileWriter(file);
                myWriter.write(getDefaultFileContent());
                myWriter.close();
            } catch (IOException e) {
                MainLogger.getLogger().error("Unable to create Config " + this.file.toString(), e);
            }
        }

        this.load();
    }

    public void load() {
        load(null);
    }

    public abstract void load(InputStream inputStream);

    public abstract void save();

    public void loadFrom(Map<String, Object> values) {
        this.values = values;
    }

    public Set<String> getKeys() {
        return this.values.keySet();
    }

    public void remove(String key) {
        LastMap lastMap = getLastMap(key);

        if (lastMap == null) return;

        lastMap.map.remove(lastMap.key);
    }

    public void set(String key, Object value) {
        LastMap lastMap = getLastMap(key);

        if (lastMap == null) return;

        lastMap.map.put(lastMap.key, value);
    }

    @SuppressWarnings("unchecked")
    private LastMap getLastMap(String key) {
        if (key == null || key.isEmpty()) return null;

        Map<String, Object> values = this.values;
        String[] keys = key.split("\\.");
        String currentKey = null;

        for (int i = 0; i < keys.length; i++) {
            currentKey = keys[i];

            if (i + 1 < keys.length && values.get(currentKey) == null) {
                values.put(currentKey, new LinkedHashMap<>());
            }

            if (!(values.get(currentKey) instanceof Map)) {
                break;
            }

            values = (Map<String, Object>) values.get(currentKey);
        }

        return new LastMap(currentKey, values);
    }

    public Object get(String key) {
        return this.get(key, null);
    }

    @SuppressWarnings("unchecked")
    public Object get(String key, Object defaultValue) {
        if (key == null || key.isEmpty()) return defaultValue;

        Map<String, Object> values = this.values;
        String[] keys = key.split("\\.");

        if (this.values.containsKey(key)) {
            return this.values.get(key);
        }

        for (int i = 0; i < keys.length; i++) {
            Object currentValue = values.get(keys[i]);

            if (currentValue == null) {
                return defaultValue;
            }

            if (i + 1 == keys.length) {
                return currentValue;
            }

            values = (Map<String, Object>) currentValue;
        }

        return defaultValue;
    }

    public boolean exists(String key) {
        return get(key) != null;
    }

    public Map<String, Object> getAll() {
        return values;
    }

    public abstract String getDefaultFileContent();

    public void setString(String key, String value) {
        this.set(key, value);
    }

    public String getString(String key) {
        return this.getString(key, null);
    }

    public String getString(String key, String defaultValue) {
        return String.valueOf(this.get(key, defaultValue));
    }


    public void setInt(String key, Integer value) {
        this.set(key, value);
    }

    public Integer getInt(String key) {
        return this.getInt(key, null);
    }

    public Integer getInt(String key, Integer defaultValue) {
        return Integer.valueOf(String.valueOf(this.get(key, defaultValue)));
    }


    public void setLong(String key, Long value) {
        this.set(key, value);
    }

    public Long getLong(String key) {
        return this.getLong(key, null);
    }

    public Long getLong(String key, Long defaultValue) {
        return Long.valueOf(String.valueOf(this.get(key, defaultValue)));
    }


    public void setDouble(String key, Double value) {
        this.set(key, value);
    }

    public Double getDouble(String key) {
        return this.getDouble(key, null);
    }

    public Double getDouble(String key, Double defaultValue) {
        return Double.valueOf(String.valueOf(this.get(key, defaultValue)));

    }


    public void setBoolean(String key, Boolean value) {
        this.set(key, value);
    }

    public Boolean getBoolean(String key) {
        return getBoolean(key, null);
    }

    public Boolean getBoolean(String key, Boolean defaultValue) {
        return (Boolean) this.get(key, defaultValue);
    }


    public <T> void setList(String key, List<T> value) {
        this.set(key, value);
    }

    public <T> List<T> getList(String key) {
        return this.getList(key, null);
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getList(String key, List<T> defaultValue) {
        return (List<T>) this.get(key, defaultValue);
    }


    public void setStringList(String key, List<String> value) {
        this.values.put(key, value);
    }

    public List<String> getStringList(String key) {
        return getStringList(key, null);
    }

    @SuppressWarnings("unchecked")
    public List<String> getStringList(String key, List<String> defaultValue) {
        return (List<String>) this.get(key, defaultValue);
    }
}
