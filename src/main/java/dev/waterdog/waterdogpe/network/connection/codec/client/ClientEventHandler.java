/*
 * Copyright 2022 WaterdogTEAM
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

package dev.waterdog.waterdogpe.network.connection.codec.client;

import dev.waterdog.waterdogpe.network.connection.client.ClientConnection;
import dev.waterdog.waterdogpe.network.connection.handler.ReconnectReason;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import dev.waterdog.waterdogpe.utils.types.TranslationContainer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.cloudburstmc.netty.channel.raknet.RakDisconnectReason;

public class ClientEventHandler extends ChannelInboundHandlerAdapter {
    public static final String NAME = "client-event-handler";

    private final ProxiedPlayer player;
    private final ClientConnection connection;

    public ClientEventHandler(ProxiedPlayer player, ClientConnection connection) {
        this.player = player;
        this.connection = connection;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        this.player.onDownstreamDisconnected(this.connection);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object event) throws Exception {
        if (!(event instanceof RakDisconnectReason reason)) {
            return;
        }

        if (reason == RakDisconnectReason.TIMED_OUT) {
            this.player.onDownstreamTimeout(this.connection.getServerInfo());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (!ctx.channel().isActive()) {
            return;
        }

        this.player.getLogger().warning("[" + connection.getSocketAddress() + "|" + this.player.getName() + "] - exception caught", cause);
        this.connection.disconnect();

        TranslationContainer msg = new TranslationContainer("waterdog.downstream.down", this.connection.getServerInfo().getServerName(), cause.getMessage());
        if (this.player.sendToFallback(this.connection.getServerInfo(), ReconnectReason.EXCEPTION, cause.getMessage())) {
            this.player.sendMessage(msg);
        } else {
            this.player.disconnect(msg);
        }
    }
}
