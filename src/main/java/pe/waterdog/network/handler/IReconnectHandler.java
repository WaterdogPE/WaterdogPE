package pe.waterdog.network.handler;

import pe.waterdog.network.ServerInfo;
import pe.waterdog.player.ProxiedPlayer;

public interface IReconnectHandler {

    ServerInfo getFallbackServer(ProxiedPlayer p, ServerInfo oldServer);
}
