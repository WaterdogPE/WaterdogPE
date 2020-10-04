package pe.waterdog.plugin;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;
import pe.waterdog.ProxyServer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class PluginManager {

    private final ProxyServer server;
    private final Map<String, Plugin> pluginMap = new HashMap<>();
    private final Yaml yamlLoader = new Yaml(new CustomClassLoaderConstructor(this.getClass().getClassLoader()));

    public PluginManager(ProxyServer server) {
        this.server = server;
        PluginLoader.loadPluginsIn(this.server.getPluginPath(), this);
    }

    public Plugin loadPlugin(Path p, boolean directStartup) {
        if (!Files.isRegularFile(p) || !PluginLoader.isJarFile(p)) {
            this.server.getLogger().warning("Cannot load plugin: Provided file is no jar file: " + p.getFileName());
            return null;
        }

        File pluginFile = p.toFile();
        if (pluginFile.exists()) {
            PluginYAML config = this.loadPluginData(pluginFile, this.yamlLoader);
            if (config == null) return null;

            if (this.getPluginByName(config.getName()) != null) {
                this.server.getLogger().warning("Plugin is already loaded: " + config.getName());
                return null;
            }

            Plugin plugin = this.loadPluginJAR(config, pluginFile);
            if (plugin != null) {
                this.server.getLogger().info("Loaded plugin " + config.getName() + " successfully! (version=" + config.getVersion() + ",author=" + config.getAuthor() + ")");
                this.pluginMap.put(config.getName(), plugin);

                plugin.onStartup();
                if (directStartup) {
                    plugin.onEnable();
                }
                return plugin;
            }
        }
        return null;
    }

    public Plugin loadPlugin(Path p) {
        return this.loadPlugin(p, false);
    }

    private PluginYAML loadPluginData(File file, Yaml yaml) {
        try (JarFile pluginJar = new JarFile(file)) {
            JarEntry configEntry = pluginJar.getJarEntry("plugin.yml");
            if (configEntry != null) {
                InputStream fileStream = pluginJar.getInputStream(configEntry);
                PluginYAML pluginConfig = yaml.loadAs(fileStream, PluginYAML.class);
                if (pluginConfig.getMain() != null && pluginConfig.getName() != null) {
                    // Valid plugin.yml, main and name set
                    return pluginConfig;

                } else {
                    this.server.getLogger().warning("Invalid plugin.yml for " + file.getName() + ": main and/or name property missing");
                }
            } else {
                this.server.getLogger().warning("Jar file " + file.getName() + " doesnt contain a plugin.yml!");
            }
        } catch (IOException e) {
            this.server.getLogger().error("Error while reading plugin directory", e);
        }
        return null;
    }


    private Plugin loadPluginJAR(PluginYAML pluginConfig, File pluginJar) {
        try {
            URLClassLoader loader = new URLClassLoader(new URL[]{pluginJar.toURI().toURL()}, this.getClass().getClassLoader());
            try {

                Class<?> mainClass = loader.loadClass(pluginConfig.getMain());

                if (Plugin.class.isAssignableFrom(mainClass)) {
                    // Main Class extends Plugin class
                    // TODO Init Method?
                    Class<? extends Plugin> castedMain = mainClass.asSubclass(Plugin.class);
                    return castedMain.getDeclaredConstructor(PluginYAML.class, ProxyServer.class).newInstance(pluginConfig, this.server);
                }
            } catch (ClassCastException e) {
                this.server.getLogger().error("Error while loading plugin main class(main=" + pluginConfig.getMain() + ",plugin=" + pluginConfig.getName() + ")", e);
            } catch (IllegalAccessException | InstantiationException e) {
                this.server.getLogger().error("Error while creating main class instance(plugin=" + pluginConfig.getName() + ",main=" + pluginConfig.getMain() + ")", e);
            } catch (ClassNotFoundException e) {
                this.server.getLogger().error("Main Class " + pluginConfig.getMain() + " not found", e);
            } catch (NoSuchMethodException | InvocationTargetException e) {
                this.server.getLogger().error("Malformed Plugin Class Constructor", e);
            }
        } catch (MalformedURLException e) {
            this.server.getLogger().error("Error while creating class loader(plugin=" + pluginConfig.getName() + ")");
        }
        return null;
    }

    public Map<String, Plugin> getPluginMap() {
        return this.pluginMap;
    }

    public Collection<Plugin> getPlugins() {
        return this.pluginMap.values();
    }

    public Plugin getPluginByName(String pluginName) {
        return this.pluginMap.getOrDefault(pluginName, null);
    }


}
