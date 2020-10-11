package pe.waterdog.network.handler;

import pe.waterdog.network.ServerInfo;
import pe.waterdog.player.ProxiedPlayer;

public interface IJoinHandler {

    ServerInfo determineServer(ProxiedPlayer player);
}
