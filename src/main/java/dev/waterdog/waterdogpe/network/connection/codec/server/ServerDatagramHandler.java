/*
 * Copyright 2023 WaterdogTEAM
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

package dev.waterdog.waterdogpe.network.connection.codec.server;

import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.network.NetworkMetrics;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import org.cloudburstmc.netty.channel.raknet.RakServerChannel;
import org.cloudburstmc.netty.util.nethernet.IpRangeSet;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class ServerDatagramHandler extends ChannelInboundHandlerAdapter {
    public static final String NAME = "server-datagram-handler";

    private final ProxyServer proxy;
    private final boolean proxied;
    private final IpRangeSet proxies;

    /**
     * @param proxied Whether PROXY protocol is on, which accepts datagrams from the listed proxies alone
     * @param proxies The PROXY protocol proxies datagrams are accepted from
     */
    public ServerDatagramHandler(ProxyServer proxy, boolean proxied, IpRangeSet proxies) {
        this.proxy = proxy;
        this.proxied = proxied;
        this.proxies = proxies;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof DatagramPacket packet && (!this.isFromAllowedProxy(packet.sender())
                || this.proxy.getSecurityManager().isAddressBlocked(this.resolveSource(ctx, packet.sender())))) {
            NetworkMetrics metrics = this.proxy.getNetworkMetrics();
            if (metrics != null) {
                metrics.droppedBytes(packet.content().readableBytes());
            }
            packet.release();
            return; // drop any incoming messages
        }
        ctx.fireChannelRead(msg);
    }

    /** A PROXY header from anywhere else could claim any client address. */
    private boolean isFromAllowedProxy(InetSocketAddress sender) {
        return !this.proxied || this.proxies.contains(sender);
    }

    /**
     * Resolve a datagram's sender to the address bans are recorded against.
     * <p>
     * When running behind HAProxy (PROXY protocol enabled) datagrams arrive from the load balancer's
     * address, while bans are keyed on the real client address (the RakNet child channel's remote
     * address, which the library substitutes from the PROXY header). Without translating here, blocked
     * clients would never be dropped. We resolve the sender to the real client via the parent
     * {@link RakServerChannel}'s proxy-&gt;client mapping, falling back to the raw sender when PROXY
     * protocol is disabled or the mapping has not been established yet.
     */
    private InetAddress resolveSource(ChannelHandlerContext ctx, InetSocketAddress sender) {
        if (ctx.channel() instanceof RakServerChannel rakServerChannel) {
            InetSocketAddress realClient = rakServerChannel.getClientAddress(sender);
            if (realClient != null) {
                return realClient.getAddress();
            }
        }
        return sender.getAddress();
    }
}
