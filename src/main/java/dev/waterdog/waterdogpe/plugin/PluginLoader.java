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

package dev.waterdog.waterdogpe.plugin;

import lombok.extern.log4j.Log4j2;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Log4j2
public class PluginLoader {

    private final PluginManager pluginManager;

    public PluginLoader(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    protected static boolean isJarFile(Path file) {
        return file.getFileName().toString().endsWith(".jar");
    }

    protected PluginClassLoader loadClassLoader(PluginYAML pluginConfig, File pluginJar) {
        try {
            return new PluginClassLoader(this.pluginManager, this.getClass().getClassLoader(), pluginJar);
        } catch (MalformedURLException e) {
            log.error("Error while creating class loader(plugin={})", pluginConfig.getName());
        }
        return null;
    }

    protected Plugin loadPluginJAR(PluginYAML pluginConfig, File pluginJar, PluginClassLoader loader) {
        try {
            Class<?> mainClass = loader.loadClass(pluginConfig.getMain());
            if (!Plugin.class.isAssignableFrom(mainClass)) {
                return null;
            }

            Class<? extends Plugin> castedMain = mainClass.asSubclass(Plugin.class);
            Plugin plugin = castedMain.getDeclaredConstructor().newInstance();
            plugin.init(pluginConfig, this.pluginManager.getProxy(), pluginJar);
            return plugin;
        } catch (Exception e) {
            log.error("Error while loading plugin main class(main={}, plugin={})", pluginConfig.getMain(), pluginConfig.getName(), e);
        }
        return null;
    }

    protected PluginYAML loadPluginData(File file, Yaml yaml) {
        try (JarFile pluginJar = new JarFile(file)) {
            JarEntry configEntry = pluginJar.getJarEntry("waterdog.yml");
            if (configEntry == null) {
                configEntry = pluginJar.getJarEntry("plugin.yml");
            }

            if (configEntry == null) {
                log.warn("Jar file " + file.getName() + " doesnt contain a waterdog.yml or plugin.yml!");
                return null;
            }

            try (InputStream fileStream = pluginJar.getInputStream(configEntry)) {
                PluginYAML pluginConfig = yaml.loadAs(fileStream, PluginYAML.class);
                if (pluginConfig.getMain() != null && pluginConfig.getName() != null) {
                    // Valid plugin.yml, main and name set
                    return pluginConfig;
                }
            }
            log.warn("Invalid plugin.yml for " + file.getName() + ": main and/or name property missing");
        } catch (Exception e) {
            log.error("Can not load plugin files in " + file.getPath(), e);
        }
        return null;
    }
}
