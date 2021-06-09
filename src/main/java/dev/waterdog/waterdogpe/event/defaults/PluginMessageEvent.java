package dev.waterdog.waterdogpe.event.defaults;

import com.google.common.io.ByteArrayDataInput;
import dev.waterdog.waterdogpe.event.defaults.PlayerEvent;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;

public class PluginMessageEvent extends PlayerEvent {
    private final byte[] data;
    private final String channel;

    public PluginMessageEvent(ProxiedPlayer player, byte[] data, String channel) {
        super(player);
        this.data = data;
        this.channel = channel;
    }

    public byte[] getData() {
        return data != null ? data.clone() : null;
    }

    public String getChannel() {
        return channel;
    }
}
