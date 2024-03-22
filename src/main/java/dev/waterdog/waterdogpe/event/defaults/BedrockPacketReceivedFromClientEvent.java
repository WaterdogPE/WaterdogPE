package dev.waterdog.waterdogpe.event.defaults;

import dev.waterdog.waterdogpe.event.CancellableEvent;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;

public class BedrockPacketReceivedFromClientEvent extends PlayerEvent implements CancellableEvent {

    private BedrockPacket packet;

    public BedrockPacketReceivedFromClientEvent(ProxiedPlayer player, BedrockPacket packet) {
        super(player);
        this.packet = packet;
    }

    public BedrockPacket getPacket() {
        return this.packet;
    }

}