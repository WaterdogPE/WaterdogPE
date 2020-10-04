package pe.waterdog.event.events;

import pe.waterdog.event.CancellableEvent;
import pe.waterdog.network.ServerInfo;
import pe.waterdog.player.ProxiedPlayer;

public class PreTransferEvent extends CancellableEvent {

    private ProxiedPlayer player;

    private ServerInfo targetServer;

    public PreTransferEvent(ProxiedPlayer player, ServerInfo targetServer) {
        this.player = player;
        this.targetServer = targetServer;
    }

    public ProxiedPlayer getPlayer() {
        return player;
    }

    public void setPlayer(ProxiedPlayer player) {
        this.player = player;
    }

    public ServerInfo getTargetServer() {
        return targetServer;
    }

    public void setTargetServer(ServerInfo targetServer) {
        this.targetServer = targetServer;
    }
}
