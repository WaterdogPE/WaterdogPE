package pe.waterdog.event.events;

import pe.waterdog.event.CancellableEvent;
import pe.waterdog.player.ProxiedPlayer;

public class PlayerChatEvent extends CancellableEvent {

    private ProxiedPlayer player;

    private String message;

    public PlayerChatEvent(ProxiedPlayer player, String message) {
        this.player = player;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ProxiedPlayer getPlayer() {
        return player;
    }
}
