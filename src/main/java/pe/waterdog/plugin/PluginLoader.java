package pe.waterdog.plugin;

import org.yaml.snakeyaml.Yaml;
import pe.waterdog.logger.Logger;

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

    public PluginLoader(PluginManager pluginManager){
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
                Logger.getLogger().error("Error while loading plugin main class(main=" + pluginConfig.getMain() + ",plugin=" + pluginConfig.getName() + ")", e);
            } catch (IllegalAccessException | InstantiationException e) {
                Logger.getLogger().error("Error while creating main class instance(plugin=" + pluginConfig.getName() + ",main=" + pluginConfig.getMain() + ")", e);
            } catch (ClassNotFoundException e) {
                Logger.getLogger().error("Main Class " + pluginConfig.getMain() + " not found", e);
            }
        } catch (MalformedURLException e) {
            Logger.getLogger().error("Error while creating class loader(plugin=" + pluginConfig.getName() + ")");
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
                    Logger.getLogger().warning("Invalid plugin.yml for " + file.getName() + ": main and/or name property missing");
                }
            } else {
                Logger.getLogger().warning("Jar file " + file.getName() + " doesnt contain a plugin.yml!");
            }
        } catch (IOException e) {
            Logger.getLogger().error("Error while reading plugin directory", e);
        }
        return null;
    }
}
