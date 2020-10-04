package pe.waterdog.plugin;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;
import pe.waterdog.ProxyServer;
import pe.waterdog.logger.Logger;
import pe.waterdog.utils.exceptions.PluginChangeStateException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class PluginManager {

    private final ProxyServer proxy;
    private final PluginLoader pluginLoader;

    private final Map<String, Plugin> pluginMap = new HashMap<>();
    private final Yaml yamlLoader = new Yaml(new CustomClassLoaderConstructor(this.getClass().getClassLoader()));

    public PluginManager(ProxyServer proxy) {
        this.proxy = proxy;
        this.pluginLoader = new PluginLoader(this);
        this.loadPluginsIn(this.proxy.getPluginPath());
    }

    public void loadPluginsIn(Path folderPath) {
        this.loadPluginsIn(folderPath, false);
    }

    public void loadPluginsIn(Path folderPath, boolean directStartup) {
        try (Stream<Path> pluginPaths = Files.walk(folderPath)) {
            pluginPaths
                    .filter(Files::isRegularFile)
                    .filter(PluginLoader::isJarFile)
                    .forEach(jarPath -> {
                        this.loadPlugin(jarPath, directStartup);
                    });
        } catch (IOException e) {
            Logger.getLogger().error("Error while filtering plugin files", e);
        }
    }

    public Plugin loadPlugin(Path p) {
        return this.loadPlugin(p, false);
    }

    public Plugin loadPlugin(Path p, boolean directStartup) {
        if (!Files.isRegularFile(p) || !PluginLoader.isJarFile(p)) {
            this.proxy.getLogger().warning("Cannot load plugin: Provided file is no jar file: " + p.getFileName());
            return null;
        }

        File pluginFile = p.toFile();
        if (pluginFile.exists()) {
            PluginYAML config = this.pluginLoader.loadPluginData(pluginFile, this.yamlLoader);
            if (config == null) return null;

            if (this.getPluginByName(config.getName()) != null) {
                this.proxy.getLogger().warning("Plugin is already loaded: " + config.getName());
                return null;
            }

            Plugin plugin = this.pluginLoader.loadPluginJAR(config, pluginFile);
            if (plugin != null) {
                this.proxy.getLogger().info("Loaded plugin " + config.getName() + " successfully! (version=" + config.getVersion() + ",author=" + config.getAuthor() + ")");
                this.pluginMap.put(config.getName(), plugin);

                plugin.onStartup();
                if (directStartup) {
                    try {
                        plugin.setEnabled(true);
                    }catch (Exception e){
                        this.proxy.getLogger().error("Direct startup failed!", e);
                    }
                }
                return plugin;
            }
        }
        return null;
    }

    public void enableAllPlugins(){
        LinkedList<Plugin> failed = new LinkedList<>();

        for (Plugin plugin : this.pluginMap.values()){
            if (!this.enablePlugin(plugin, null)){
                failed.add(plugin);
            }
        }

        if (!failed.isEmpty()){
            StringBuilder builder = new StringBuilder("§cFailed to load plugins: §e");
            while (failed.peek() != null){
                Plugin plugin = failed.poll();
                builder.append(plugin.getName());
                if (failed.peek() != null){
                    builder.append(", ");
                }
            }
            this.proxy.getLogger().warning(builder.toString());
        }
    }

    public boolean enablePlugin(Plugin plugin, String parent){
        if (plugin.isEnabled()) return true;
        String pluginName = plugin.getName();

        if (plugin.getDescription().getDepends() != null){
            for (String depend : plugin.getDescription().getDepends()){
                if (depend.equals(parent)){
                    this.proxy.getLogger().warning("§cCan not enable plugin "+pluginName+" circular dependency "+parent+"!");
                    return false;
                }

                Plugin dependPlugin = this.getPluginByName(depend);
                if (dependPlugin == null){
                    this.proxy.getLogger().warning("§cCan not enable plugin "+pluginName+" missing dependency "+depend+"!");
                    return false;
                }

                if (!dependPlugin.isEnabled() && !this.enablePlugin(dependPlugin, pluginName)){
                    return false;
                }
            }
        }

        try {
            plugin.setEnabled(true);
        }catch (PluginChangeStateException e){
            this.proxy.getLogger().error(e.getMessage(), e.getCause());
            return false;
        }
        return true;
    }

    public void disableAllPlugins(){
        for (Plugin plugin : this.pluginMap.values()){
            this.proxy.getLogger().info("Disabling plugin "+plugin.getName()+"!");
            try {
                plugin.setEnabled(false);
            }catch (PluginChangeStateException e){
                this.proxy.getLogger().error(e.getMessage(), e.getCause());
            }
        }
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

    public ProxyServer getProxy() {
        return this.proxy;
    }
}
