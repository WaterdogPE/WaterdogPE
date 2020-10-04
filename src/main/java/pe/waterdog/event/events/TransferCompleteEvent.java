package pe.waterdog.event.events;

import pe.waterdog.event.AsyncEvent;
import pe.waterdog.event.Event;
import pe.waterdog.network.session.ServerConnection;
import pe.waterdog.player.ProxiedPlayer;

@AsyncEvent
public class TransferCompleteEvent extends Event {

    private ServerConnection oldServer;
    private ServerConnection newServer;
    private ProxiedPlayer player;

    public TransferCompleteEvent(ServerConnection oldServer, ServerConnection newServer, ProxiedPlayer player) {
        this.oldServer = oldServer;
        this.newServer = newServer;
        this.player = player;
    }


    public ServerConnection getNewServer() {
        return newServer;
    }

    public ServerConnection getOldServer() {
        return oldServer;
    }

    public ProxiedPlayer getPlayer() {
        return player;
    }
}

