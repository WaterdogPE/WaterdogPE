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

package pe.waterdog.plugin;

import org.yaml.snakeyaml.Yaml;
import pe.waterdog.logger.MainLogger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
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
        try {
            URLClassLoader loader = new URLClassLoader(new URL[]{pluginJar.toURI().toURL()}, this.getClass().getClassLoader());
            try {
                Class<?> mainClass = loader.loadClass(pluginConfig.getMain());

                if (Plugin.class.isAssignableFrom(mainClass)) {
                    // Main Class extends Plugin class
                    // TODO Init Method?
                    Class<? extends Plugin> castedMain = mainClass.asSubclass(Plugin.class);
                    Plugin plugin = castedMain.newInstance();
                    plugin.init(pluginConfig, this.pluginManager.getProxy(), pluginJar);
                    return plugin;
                }
            } catch (ClassCastException e) {
                MainLogger.getLogger().error("Error while loading plugin main class(main=" + pluginConfig.getMain() + ",plugin=" + pluginConfig.getName() + ")", e);
            } catch (IllegalAccessException | InstantiationException e) {
                MainLogger.getLogger().error("Error while creating main class instance(plugin=" + pluginConfig.getName() + ",main=" + pluginConfig.getMain() + ")", e);
            } catch (ClassNotFoundException e) {
                MainLogger.getLogger().error("Main Class " + pluginConfig.getMain() + " not found", e);
            }
        } catch (MalformedURLException e) {
            MainLogger.getLogger().error("Error while creating class loader(plugin=" + pluginConfig.getName() + ")");
        }
        return null;
    }

    protected PluginYAML loadPluginData(File file, Yaml yaml) {
        try (JarFile pluginJar = new JarFile(file)) {
            JarEntry configEntry = pluginJar.getJarEntry("plugin.yml");
            if (configEntry != null) {
                InputStream fileStream = pluginJar.getInputStream(configEntry);
                PluginYAML pluginConfig = yaml.loadAs(fileStream, PluginYAML.class);
                if (pluginConfig.getMain() != null && pluginConfig.getName() != null) {
                    // Valid plugin.yml, main and name set
                    return pluginConfig;

                } else {
                    MainLogger.getLogger().warning("Invalid plugin.yml for " + file.getName() + ": main and/or name property missing");
                }
            } else {
                MainLogger.getLogger().warning("Jar file " + file.getName() + " doesnt contain a plugin.yml!");
            }
        } catch (IOException e) {
            MainLogger.getLogger().error("Error while reading plugin directory", e);
        }
        return null;
    }
}
