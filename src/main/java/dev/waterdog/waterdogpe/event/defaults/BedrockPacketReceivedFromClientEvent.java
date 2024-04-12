package dev.waterdog.waterdogpe.event.defaults;

import dev.waterdog.waterdogpe.event.CancellableEvent;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import lombok.Getter;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;

@Getter
public class BedrockPacketReceivedFromClientEvent extends PlayerEvent implements CancellableEvent {

    private final BedrockPacket packet;

    public BedrockPacketReceivedFromClientEvent(ProxiedPlayer player, BedrockPacket packet) {
        super(player);

        this.packet = packet;
    }
}