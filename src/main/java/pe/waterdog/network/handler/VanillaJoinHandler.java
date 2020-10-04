package pe.waterdog.network.handler;

import pe.waterdog.ProxyServer;
import pe.waterdog.network.ServerInfo;

public class VanillaJoinHandler implements IJoinHandler {

    private final ProxyServer server;

    public VanillaJoinHandler(ProxyServer server) {
        this.server = server;
    }

    @Override
    public ServerInfo determineServer() {
        return this.server.getServer(this.server.getConfiguration().getPriorities().get(0));
    }
}
