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

package dev.waterdog.waterdogpe.network.connection.codec.initializer;

import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.network.connection.codec.query.QueryHandler;
import dev.waterdog.waterdogpe.network.connection.codec.server.RakNetPingHandler;
import dev.waterdog.waterdogpe.network.connection.codec.server.ServerDatagramHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import org.cloudburstmc.netty.handler.codec.raknet.common.UnconnectedPongEncoder;
import org.cloudburstmc.netty.handler.codec.raknet.server.RakServerOfflineHandler;

public class OfflineServerChannelInitializer extends ChannelInitializer<Channel> {
    private final ProxyServer proxy;

    public OfflineServerChannelInitializer(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Override
    protected void initChannel(Channel channel) throws Exception {
        channel.pipeline()
                .addFirst(ServerDatagramHandler.NAME, new ServerDatagramHandler(this.proxy.getSecurityManager()))
                .addAfter(RakServerOfflineHandler.NAME, RakNetPingHandler.NAME, new RakNetPingHandler(this.proxy));
        if (this.proxy.getQueryHandler() != null) {
            channel.pipeline().addAfter(UnconnectedPongEncoder.NAME, QueryHandler.NAME, this.proxy.getQueryHandler());
        }
    }
}
