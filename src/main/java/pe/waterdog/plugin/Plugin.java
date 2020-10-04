package pe.waterdog.plugin;

import com.google.common.base.Preconditions;
import pe.waterdog.ProxyServer;
import pe.waterdog.utils.Configuration;
import pe.waterdog.utils.FileUtils;
import pe.waterdog.utils.YamlConfig;
import pe.waterdog.utils.exceptions.PluginChangeStateException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public abstract class Plugin {

    private final PluginYAML description;
    private final ProxyServer proxy;

    private final File dataFolder;
    private final File configFile;

    private Configuration config;
    protected boolean enabled = false;

    public Plugin(PluginYAML description, ProxyServer proxy) {
        this.description = description;
        this.proxy = proxy;

        this.dataFolder = new File(proxy.getPluginPath()+"/"+description.getName().toLowerCase()+"/");
        if (!this.dataFolder.exists()){
            this.dataFolder.mkdirs();
        }
        this.configFile = new File(this.dataFolder, "config.yml");
    }

    public void onStartup(){
    }
    public abstract void onEnable();
    public void onDisable(){
    }

    public void setEnabled(boolean enabled) throws PluginChangeStateException {
        if (this.enabled == enabled){
            return;
        }
        this.enabled = enabled;
        try {
            if (enabled){
                this.onEnable();
            }else {
                this.onDisable();
            }
        }catch (Exception e){
            throw new PluginChangeStateException("Can not "+(enabled? "enable" : "disable")+" plugin "+this.description.getName()+"!", e);
        }
    }

    //TODO: do plugin class loader
    public InputStream getResourceFile(String filename) {
        return this.getClass().getClassLoader().getResourceAsStream(filename);
    }

    public boolean saveResource(String filename) {
        return this.saveResource(filename, false);
    }

    public boolean saveResource(String filename, boolean replace) {
        return this.saveResource(filename, filename, replace);
    }

    public boolean saveResource(String filename, String outputName, boolean replace) {
        Preconditions.checkArgument(filename != null && !filename.trim().isEmpty(), "Filename can not be null!");

        File file = new File(this.dataFolder, outputName);
        if (file.exists() && !replace) {
            return false;
        }

        try {
            InputStream resource = this.getResourceFile(filename);
            if (resource == null) return false;

            File outFolder = file.getParentFile();
            if (!outFolder.exists()) {
                outFolder.mkdirs();
            }
            FileUtils.writeFile(file, resource);
        } catch (IOException e) {
            this.proxy.getLogger().error("Can not save plugin file!", e);
            return false;
        }
        return true;
    }

    public void loadConfig(){
        try {
            this.saveResource("config.yml");
            this.config = new YamlConfig(this.configFile);
        }catch (Exception e){
            this.proxy.getLogger().error("Can not load plugin config!");
        }
    }

    public Configuration getConfig(){
        if (this.config == null){
            this.loadConfig();
        }
        return this.config;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public PluginYAML getDescription() {
        return this.description;
    }

    public String getName(){
        return this.description.getName();
    }

    public ProxyServer getProxy() {
        return this.proxy;
    }

    public File getDataFolder() {
        return this.dataFolder;
    }
}
