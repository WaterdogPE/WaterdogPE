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

package dev.waterdog.waterdogpe.plugin;

import dev.waterdog.waterdogpe.logger.MainLogger;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class PluginLoader {

    private final PluginManager pluginManager;

    public PluginLoader(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    protected static boolean isJarFile(Path file) {
        return file.getFileName().toString().endsWith(".jar");
    }

    protected Plugin loadPluginJAR(PluginYAML pluginConfig, File pluginJar) {
        PluginClassLoader loader;
        try {
            loader = new PluginClassLoader(this.pluginManager, this.getClass().getClassLoader(), pluginJar);
            this.pluginManager.pluginClassLoaders.put(pluginConfig.getName(), loader);
        } catch (MalformedURLException e) {
            MainLogger.getLogger().error("Error while creating class loader(plugin=" + pluginConfig.getName() + ")");
            return null;
        }

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
            MainLogger.getLogger().error("Error while loading plugin main class(main=" + pluginConfig.getMain() + ",plugin=" + pluginConfig.getName() + ")", e);
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
                MainLogger.getLogger().warning("Jar file " + file.getName() + " doesnt contain a waterdog.yml or plugin.yml!");
                return null;
            }

            try (InputStream fileStream = pluginJar.getInputStream(configEntry)) {
                PluginYAML pluginConfig = yaml.loadAs(fileStream, PluginYAML.class);
                if (pluginConfig.getMain() != null && pluginConfig.getName() != null) {
                    // Valid plugin.yml, main and name set
                    return pluginConfig;
                }
            }
            MainLogger.getLogger().warning("Invalid plugin.yml for " + file.getName() + ": main and/or name property missing");
        } catch (Exception e) {
            MainLogger.getLogger().error("Can not load plugin files in " + file.getPath(), e);
        }
        return null;
    }
}
