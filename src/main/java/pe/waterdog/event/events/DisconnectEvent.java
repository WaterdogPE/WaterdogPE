package pe.waterdog.event.events;

import pe.waterdog.event.AsyncEvent;
import pe.waterdog.event.Event;
import pe.waterdog.player.ProxiedPlayer;

@AsyncEvent
public class DisconnectEvent extends Event {

    private ProxiedPlayer player;

    public DisconnectEvent(ProxiedPlayer player) {
        this.player = player;
    }

    public ProxiedPlayer getPlayer() {
        return player;
    }
}
