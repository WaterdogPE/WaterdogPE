package pe.waterdog.utils.types;

import pe.waterdog.network.ServerInfo;
import pe.waterdog.player.ProxiedPlayer;

/**
 * Called whenever a client is being kicked from a downstream server
 * Can be used to easily setup a fallback to transfer the player to another server
 */
public interface IReconnectHandler {

    /**
     * @param player    the player who got kicked by downstream
     * @param oldServer the ServerInfo of the downstream server who kicked the player
     * @return a ServerInfo if there was a valid server found for fallback, or null if no server was found. null will lead to the player getting kicked.
     */
    ServerInfo getFallbackServer(ProxiedPlayer player, ServerInfo oldServer, String kickMessage);
}
