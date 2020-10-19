package pe.waterdog.utils.types;

import pe.waterdog.network.ServerInfo;
import pe.waterdog.player.ProxiedPlayer;

public class VanillaReconnectHandler implements IReconnectHandler {

    @Override
    public ServerInfo getFallbackServer(ProxiedPlayer player, ServerInfo oldServer, String message) {
        return null;
    }
}
