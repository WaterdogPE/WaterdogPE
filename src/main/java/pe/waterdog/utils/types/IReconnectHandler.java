package pe.waterdog.utils.types;

import pe.waterdog.network.ServerInfo;
import pe.waterdog.player.ProxiedPlayer;

public interface IReconnectHandler {

    ServerInfo getFallbackServer(ProxiedPlayer p, ServerInfo oldServer);
}
