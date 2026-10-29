/*
 * Copyright 2026 WaterdogTEAM
 * Licensed under the GNU General Public License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.waterdog.waterdogpe.network;

import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.network.connection.codec.server.ServerDatagramHandler;
import dev.waterdog.waterdogpe.security.SecurityManager;
import dev.waterdog.waterdogpe.utils.config.proxy.ProxyConfig;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.socket.DatagramPacket;
import org.cloudburstmc.netty.util.nethernet.IpRangeSet;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProxyProtocolWhitelistTest {

    /** A proxy server reading these network settings. */
    private static ProxyServer proxy(String settings) throws Exception {
        File file = Files.createTempDirectory("waterdog-config").resolve("config.yml").toFile();
        Files.writeString(file.toPath(), "network_settings:\n" + settings.indent(2));
        ProxyConfig config = new ProxyConfig(file);
        config.init();

        ProxyServer proxy = mock(ProxyServer.class);
        when(proxy.getNetworkSettings()).thenReturn(config.getNetworkSettings());
        when(proxy.getProxyProtocolProxies()).thenCallRealMethod();
        when(proxy.getWebrtcProxyProtocolRelays()).thenCallRealMethod();
        return proxy;
    }

    @Test
    public void listingProxiesTurnsProxyProtocolOn() throws Exception {
        ProxyServer proxy = proxy("""
                proxy_protocol_whitelisted_ips: ['10.0.0.0/8']
                """);

        assertTrue(proxy.getNetworkSettings().proxyProtocol());
        assertTrue(proxy.getProxyProtocolProxies().contains(InetAddress.getByName("10.1.2.3")));
        assertFalse(proxy.getProxyProtocolProxies().contains(InetAddress.getByName("192.168.1.1")));
    }

    @Test
    public void anEmptyListLeavesProxyProtocolOff() throws Exception {
        ProxyServer proxy = proxy("""
                proxy_protocol_whitelisted_ips: [' ']
                """);

        assertFalse(proxy.getNetworkSettings().proxyProtocol());
        assertTrue(proxy.getProxyProtocolProxies().isEmpty());
    }

    @Test
    public void webrtcRelaysAreTheirOwnList() throws Exception {
        ProxyServer proxy = proxy("""
                proxy_protocol_whitelisted_ips: ['10.0.0.0/8']
                webrtc_proxy_protocol_relays: ['192.0.2.0/24']
                """);

        assertTrue(proxy.getWebrtcProxyProtocolRelays().contains(InetAddress.getByName("192.0.2.1")));
        assertFalse(proxy.getWebrtcProxyProtocolRelays().contains(InetAddress.getByName("10.1.2.3")));
        assertFalse(proxy.getProxyProtocolProxies().contains(InetAddress.getByName("192.0.2.1")));
    }

    @Test
    public void aListThatParsedToNothingStillTurnsProxyProtocolOn() throws Exception {
        // An invalid prefix is skipped without a DNS lookup
        ProxyServer proxy = proxy("""
                proxy_protocol_whitelisted_ips: ['10.0.0.0/99']
                """);

        assertTrue(proxy.getNetworkSettings().proxyProtocol());
        assertTrue(proxy.getProxyProtocolProxies().isEmpty());
    }

    @Test
    public void rakNetDropsDatagramsFromOutsideTheList() throws Exception {
        EmbeddedChannel channel = rakNet(true, IpRangeSet.parse(List.of("10.0.0.0/8")));

        DatagramPacket listed = datagram("10.1.2.3");
        channel.writeInbound(listed);
        assertSame(listed, channel.readInbound());

        DatagramPacket unlisted = datagram("192.168.1.1");
        channel.writeInbound(unlisted);
        assertNull(channel.readInbound());
        assertEquals(0, unlisted.refCnt());
        listed.release();
    }

    @Test
    public void rakNetTakesAnySenderWhileNoProxyIsListed() throws Exception {
        EmbeddedChannel channel = rakNet(false, IpRangeSet.empty());

        DatagramPacket packet = datagram("192.168.1.1");
        channel.writeInbound(packet);
        assertSame(packet, channel.readInbound());
        packet.release();
    }

    @Test
    public void rakNetRefusesEveryoneWhenNoListedProxyCouldBeRead() throws Exception {
        EmbeddedChannel channel = rakNet(true, IpRangeSet.empty());

        DatagramPacket packet = datagram("10.1.2.3");
        channel.writeInbound(packet);
        assertNull(channel.readInbound());
        assertEquals(0, packet.refCnt());
    }

    private static EmbeddedChannel rakNet(boolean proxied, IpRangeSet proxies) {
        ProxyServer proxy = mock(ProxyServer.class);
        when(proxy.getSecurityManager()).thenReturn(mock(SecurityManager.class));
        return new EmbeddedChannel(new ServerDatagramHandler(proxy, proxied, proxies));
    }

    private static DatagramPacket datagram(String sender) throws Exception {
        return new DatagramPacket(Unpooled.wrappedBuffer(new byte[] {1}), new InetSocketAddress(19132),
                new InetSocketAddress(InetAddress.getByName(sender), 40000));
    }
}
