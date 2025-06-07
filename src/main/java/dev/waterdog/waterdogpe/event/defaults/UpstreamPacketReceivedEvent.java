package dev.waterdog.waterdogpe.event.defaults;

import dev.waterdog.waterdogpe.event.CancellableEvent;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;

public class UpstreamPacketReceivedEvent extends PlayerEvent implements CancellableEvent {
    private final BedrockPacket packet;

    public UpstreamPacketReceivedEvent(ProxiedPlayer player, BedrockPacket packet) {
        super(player);

        this.packet = packet;
    }

    public BedrockPacket getPacket() {
        return packet;
    }
}