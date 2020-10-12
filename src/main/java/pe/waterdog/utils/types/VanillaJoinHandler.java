package pe.waterdog.utils.types;

import pe.waterdog.ProxyServer;
import pe.waterdog.network.ServerInfo;
import pe.waterdog.player.ProxiedPlayer;
import pe.waterdog.utils.types.IJoinHandler;

public class VanillaJoinHandler implements IJoinHandler {

    private final ProxyServer server;

    public VanillaJoinHandler(ProxyServer server) {
        this.server = server;
    }

    @Override
    public ServerInfo determineServer(ProxiedPlayer player) {
        return this.server.getServer(this.server.getConfiguration().getPriorities().get(0));
    }
}
