package pe.waterdog.utils.types;

import pe.waterdog.network.ServerInfo;
import pe.waterdog.player.ProxiedPlayer;
import pe.waterdog.utils.types.IReconnectHandler;

public class VanillaReconnectHandler implements IReconnectHandler {

    @Override
    public ServerInfo getFallbackServer(ProxiedPlayer p, ServerInfo oldServer) {
        return null;
    }
}
