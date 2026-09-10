/*
 * Copyright 2026 WaterdogTEAM
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

package dev.waterdog.waterdogpe.utils.config.proxy;

import net.cubespace.Yamler.Config.Comment;
import net.cubespace.Yamler.Config.Comments;
import net.cubespace.Yamler.Config.Path;
import net.cubespace.Yamler.Config.YamlConfig;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A nested block of settings inside a config file.
 * <p>
 * Yamler treats anything below the root as a plain map: it writes it unordered, never reads a
 * {@link Comment} off it, and only ever fills in newly added keys at the root. A section comes out
 * in declaration order, hands its comments to the root, and picks up options added since the file
 * was written.
 */
public abstract class SettingsSection extends YamlConfig {

    private transient boolean upgraded;

    /**
     * Registers the comments of every section held by a config, so they reach the file.
     */
    public static void describeSections(YamlConfig root) {
        for (Field field : root.getClass().getDeclaredFields()) {
            SettingsSection section = read(root, field);
            if (section != null) {
                section.describe(root, path(field));
            }
        }
    }

    /**
     * Whether any section of a loaded config gained an option, meaning the file is behind and
     * should be written back.
     */
    public static boolean upgradedSections(YamlConfig root) {
        boolean upgraded = false;
        for (Field field : root.getClass().getDeclaredFields()) {
            SettingsSection section = read(root, field);
            upgraded |= section != null && section.upgraded;
        }
        return upgraded;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Map<String, Object> saveToMap(Class clazz) throws Exception {
        Map<String, Object> saved = super.saveToMap(clazz);

        Map<String, Object> ordered = new LinkedHashMap<>();
        for (Field field : clazz.getDeclaredFields()) {
            String path = path(field);
            if (!this.doSkip(field) && saved.containsKey(path)) {
                ordered.put(path, saved.remove(path));
            }
        }
        // Anything a subclass did not declare, such as an inherited field
        ordered.putAll(saved);
        return ordered;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void loadFromMap(Map section, Class clazz) throws Exception {
        super.loadFromMap(section, clazz);

        // A field the file has no key for keeps its default, which is correct but leaves the option
        // undocumented and invisible, so note that the file is behind
        for (Field field : clazz.getDeclaredFields()) {
            if (!this.doSkip(field) && !section.containsKey(path(field))) {
                this.upgraded = true;
            }
        }
        this.upgraded |= upgradedSections(this);
    }

    private void describe(YamlConfig root, String prefix) {
        for (Field field : this.getClass().getDeclaredFields()) {
            if (this.doSkip(field)) {
                continue;
            }

            String key = prefix + "." + path(field);
            Comment comment = field.getAnnotation(Comment.class);
            if (comment != null) {
                root.addComment(key, comment.value());
            }
            Comments comments = field.getAnnotation(Comments.class);
            if (comments != null) {
                for (String line : comments.value()) {
                    root.addComment(key, line);
                }
            }

            SettingsSection nested = read(this, field);
            if (nested != null) {
                nested.describe(root, key);
            }
        }
    }

    private static SettingsSection read(Object holder, Field field) {
        if (!SettingsSection.class.isAssignableFrom(field.getType()) || Modifier.isStatic(field.getModifiers())) {
            return null;
        }
        try {
            field.setAccessible(true);
            return (SettingsSection) field.get(holder);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static String path(Field field) {
        Path path = field.getAnnotation(Path.class);
        return path == null ? field.getName() : path.value();
    }
}
