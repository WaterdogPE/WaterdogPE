package pe.waterdog.plugin;

import pe.waterdog.ProxyServer;

public abstract class Plugin {

    private final PluginYAML description;
    private final ProxyServer proxy;

    public Plugin(PluginYAML description, ProxyServer proxy) {
        this.description = description;
        this.proxy = proxy;
    }

    public void onStartup(){
    }
    public abstract void onEnable();
    public void onShutdown(){
    }


    public PluginYAML getDescription() {
        return this.description;
    }

    public ProxyServer getProxy() {
        return this.proxy;
    }
}
