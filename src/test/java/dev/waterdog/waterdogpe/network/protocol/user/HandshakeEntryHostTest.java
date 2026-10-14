package dev.waterdog.waterdogpe.network.protocol.user;

import com.google.gson.JsonObject;
import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.event.EventManager;
import dev.waterdog.waterdogpe.logger.MainLogger;
import dev.waterdog.waterdogpe.network.connection.peer.BedrockServerSession;
import dev.waterdog.waterdogpe.network.connection.peer.ProxiedBedrockPeer;
import dev.waterdog.waterdogpe.network.nethernet.NetherNetInterface;
import dev.waterdog.waterdogpe.network.protocol.ProtocolVersion;
import io.netty.channel.Channel;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class HandshakeEntryHostTest {
    @Test
    void signalingHostOverridesLoginAddressOnlyWhenPresent() {
        ProxyServer proxy = mock(ProxyServer.class);
        EventManager events = mock(EventManager.class);
        NetherNetInterface nethernet = mock(NetherNetInterface.class);
        BedrockServerSession session = mock(BedrockServerSession.class);
        ProxiedBedrockPeer peer = mock(ProxiedBedrockPeer.class);
        Channel channel = mock(Channel.class);
        when(proxy.getEventManager()).thenReturn(events);
        when(proxy.getLogger()).thenReturn(mock(MainLogger.class));
        when(proxy.getNetherNetInterface()).thenReturn(nethernet);
        when(session.getPeer()).thenReturn(peer);
        when(peer.getChannel()).thenReturn(channel);

        JsonObject clientData = new JsonObject();
        clientData.addProperty("ServerAddress", "mariuswelt.bitwaves.de:19132");
        HandshakeEntry entry = new HandshakeEntry(null, clientData, "123", UUID.randomUUID(), "Player",
                "mid", true, ProtocolVersion.MINECRAFT_PE_1_26_50, null, false);

        when(nethernet.takeJoinHostname(channel)).thenReturn("bergwelt.bitwaves.de", (String) null);
        assertEquals("bergwelt.bitwaves.de", entry.buildData(session, proxy).getJoinHostname());
        assertEquals("mariuswelt.bitwaves.de", entry.buildData(session, proxy).getJoinHostname());
    }
}
