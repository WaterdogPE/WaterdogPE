package pe.waterdog.plugin;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;
import pe.waterdog.ProxyServer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

public class PluginManager {

    private final ProxyServer server;

    private final Map<String, Plugin> pluginMap = new HashMap<>();

    public PluginManager(ProxyServer server) {
        this.server = server;
        Yaml yamlLoader = new Yaml(new CustomClassLoaderConstructor(this.getClass().getClassLoader()));
        Map<PluginYAML, File> validPlugins = getJarFiles(yamlLoader);
        this.server.getLogger().info("Found " + validPlugins.size() + " valid plugin files");
        loadPluginJars(validPlugins);
        this.server.getLogger().info("Loaded " + this.pluginMap.size() + " plugin(s)");
    }

    public static boolean isJarFile(Path file) {
        return file.getFileName().toString().endsWith(".jar");
    }

    public void loadPluginJars(Map<PluginYAML, File> validPlugins) {
        PluginYAML pluginConfig;

        for (Map.Entry<PluginYAML, File> plugin : validPlugins.entrySet()) {

            pluginConfig = plugin.getKey();
            try {

                URLClassLoader loader = new URLClassLoader(new URL[]{plugin.getValue().toURI().toURL()}, this.getClass().getClassLoader());
                try {

                    Class mainClass = loader.loadClass(pluginConfig.getMain());

                    if (Plugin.class.isAssignableFrom(mainClass)) {
                        // Main Class extends Plugin class
                        // TODO Init Method
                        Class<Plugin> castedMain = mainClass.asSubclass(Plugin.class);
                        this.pluginMap.put(pluginConfig.getName(), castedMain.newInstance());
                    }

                } catch (ClassCastException e) {
                    this.server.getLogger().error("Error while loading plugin main class(main=" + pluginConfig.getMain() + ",plugin=" + plugin.getKey().getName() + ")", e);
                } catch (IllegalAccessException | InstantiationException e) {
                    this.server.getLogger().error("Error while creating main class instance(plugin=" + pluginConfig.getName() + ",main=" + plugin.getKey().getMain() + ")", e);
                } catch (ClassNotFoundException e) {
                    this.server.getLogger().error("Main Class " + pluginConfig.getMain() + " not found", e);
                }
            } catch (MalformedURLException e) {
                this.server.getLogger().error("Error while creating class loader(plugin=" + pluginConfig.getName() + ")");
            }

        }
    }

    public Map<PluginYAML, File> getJarFiles(Yaml yaml) {
        Map<PluginYAML, File> loadPlugins = new HashMap<>();
        try (Stream<Path> pluginPaths = Files.walk(this.server.getPluginPath())) {
            pluginPaths
                    .filter(Files::isRegularFile)
                    .filter(PluginManager::isJarFile)
                    .forEach(jarPath -> {
                        File pluginFile = new File(jarPath.toUri());
                        try (JarFile pluginJar = new JarFile(new File(jarPath.toAbsolutePath().toString()))) {

                            JarEntry configEntry = pluginJar.getJarEntry("plugin.yml");
                            if (configEntry != null) {
                                InputStream fileStream = pluginJar.getInputStream(configEntry);
                                PluginYAML pluginConfig = yaml.loadAs(fileStream, PluginYAML.class);
                                if (pluginConfig.getMain() != null && pluginConfig.getName() != null) {

                                    // Valid plugin.yml, main and name set
                                    loadPlugins.put(pluginConfig, pluginFile);

                                } else {
                                    this.server.getLogger().warning("Invalid plugin.yml for " + pluginFile.getName() + ": main and/or name property missing");
                                }
                            } else {
                                this.server.getLogger().warning("Jar file " + pluginFile.getName() + " doesnt contain a plugin.yml!");
                            }
                        } catch (IOException e) {
                            this.server.getLogger().error("Error while reading plugin directory", e);
                        }
                    });
        } catch (IOException e) {
            this.server.getLogger().error("Error while filtering plugin files", e);
        }
        return loadPlugins;
    }

}
