package pe.waterdog.utils.types;

import pe.waterdog.network.ServerInfo;
import pe.waterdog.player.ProxiedPlayer;

/**
 * Interface that can be implemented and assigned to the server.
 * The JoinHandler is called whenever a player establishes an initial connection.
 * It's job is to determine what the initial server of the client should be.
 */
public interface IJoinHandler {

    /**
     * determines the initial server
     *
     * @param player the player who is connecting to the server
     * @return ServerInfo if a server is found, or null if no server was found. null will lead to the player getting kicked.
     */
    ServerInfo determineServer(ProxiedPlayer player);
}
