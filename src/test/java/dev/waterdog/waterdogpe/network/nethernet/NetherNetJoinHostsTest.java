package dev.waterdog.waterdogpe.network.nethernet;

import io.netty.channel.embedded.EmbeddedChannel;
import org.cloudburstmc.netty.channel.nethernet.NetherNetChildChannel;
import org.cloudburstmc.netty.util.nethernet.PlayerInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NetherNetJoinHostsTest {
    @Test
    void hostBelongsOnlyToTheMatchingOfferAndIsConsumedOnce() {
        NetherNetJoinHosts hosts = new NetherNetJoinHosts();
        PlayerInfo bergPlayer = new PlayerInfo("123", "Player", "id", null, null, null);
        PlayerInfo mariusPlayer = new PlayerInfo("123", "Player", "id", null, null, null);
        EmbeddedChannel berg = new EmbeddedChannel();
        EmbeddedChannel marius = new EmbeddedChannel();
        berg.attr(NetherNetChildChannel.PLAYER_INFO).set(bergPlayer);
        marius.attr(NetherNetChildChannel.PLAYER_INFO).set(mariusPlayer);

        hosts.record("BergWelt.bitwaves.de:19132", bergPlayer);
        hosts.record("mariuswelt.bitwaves.de:19132", mariusPlayer);
        assertEquals("mariuswelt.bitwaves.de", hosts.take(marius));
        assertEquals("bergwelt.bitwaves.de", hosts.take(berg));
        assertNull(hosts.take(berg));
        assertNull(hosts.take(new EmbeddedChannel()));
        berg.finishAndReleaseAll();
        marius.finishAndReleaseAll();
    }

    @Test
    void rejectsMalformedHostRatherThanTrustingItForRouting() {
        assertNull(NetherNetJoinHosts.normalize(null));
        assertNull(NetherNetJoinHosts.normalize("bergwelt.bitwaves.de:bad"));
        assertNull(NetherNetJoinHosts.normalize("bergwelt.bitwaves.de/other"));
        assertEquals("bergwelt.bitwaves.de", NetherNetJoinHosts.normalize("Bergwelt.bitwaves.de:19132"));
    }
}
