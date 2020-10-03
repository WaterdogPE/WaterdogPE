package pe.waterdog.plugin;

import pe.waterdog.ProxyServer;

public abstract class Plugin {

    private final PluginYAML description;
    private final ProxyServer server;

    public Plugin(PluginYAML description, ProxyServer server) {
        this.description = description;
        this.server = server;
    }

    public abstract void onStartup();

    public abstract void onShutdown();

    public PluginYAML getDescription() {
        return description;
    }

    public ProxyServer getServer() {
        return server;
    }
}
