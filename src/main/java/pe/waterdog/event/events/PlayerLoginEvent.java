package pe.waterdog.event.events;

import pe.waterdog.event.CancellableEvent;
import pe.waterdog.player.ProxiedPlayer;

public class PlayerLoginEvent extends CancellableEvent {

    private ProxiedPlayer player;
    private String cancelReason = "Login cancelled";

    public PlayerLoginEvent(ProxiedPlayer player) {
        this.player = player;
    }

    public ProxiedPlayer getPlayer() {
        return player;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }
}
