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
import dev.waterdog.waterdogpe.security.SecurityManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;

public class ServerDatagramHandler extends ChannelInboundHandlerAdapter {
    public static final String NAME = "server-datagram-handler";

    private final SecurityManager manager;

    public ServerDatagramHandler(SecurityManager securityManager) {
        this.manager = securityManager;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof DatagramPacket packet && this.manager.isAddressBlocked(packet.sender().getAddress())) {
            NetworkMetrics metrics = ProxyServer.getInstance().getNetworkMetrics();
            if (metrics != null) {
                metrics.droppedBytes(packet.content().readableBytes());
            }
            packet.release();
            return; // drop any incoming messages
        }
        ctx.fireChannelRead(msg);
    }
}
