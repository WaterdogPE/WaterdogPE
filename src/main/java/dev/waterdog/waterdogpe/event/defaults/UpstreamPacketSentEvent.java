package dev.waterdog.waterdogpe.event.defaults;

import dev.waterdog.waterdogpe.event.CancellableEvent;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;

public class UpstreamPacketSentEvent extends PlayerEvent implements CancellableEvent {
    private final BedrockPacket packet;
    private boolean immediate = false;

    public UpstreamPacketSentEvent(ProxiedPlayer player, BedrockPacket packet) {
        super(player);

        this.packet = packet;
    }

    public UpstreamPacketSentEvent(ProxiedPlayer player, BedrockPacket packet, boolean immediate) {
        super(player);

        this.packet = packet;
        this.immediate = immediate;
    }

    public BedrockPacket getPacket() {
        return packet;
    }

    public boolean isImmediate() {
        return immediate;
    }
}