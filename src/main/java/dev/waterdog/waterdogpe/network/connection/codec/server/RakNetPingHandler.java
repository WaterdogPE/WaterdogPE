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

package dev.waterdog.waterdogpe.network.connection.codec.server;

import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.event.defaults.ProxyPingEvent;
import dev.waterdog.waterdogpe.network.protocol.ProtocolVersion;
import dev.waterdog.waterdogpe.utils.config.proxy.ProxyConfig;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.netty.channel.raknet.RakPing;
import org.cloudburstmc.netty.channel.raknet.config.RakChannelOption;

import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;

@Log4j2
public class RakNetPingHandler extends SimpleChannelInboundHandler<RakPing> {
    public static final String NAME = "rak-ping-handler";

    private final ProxyServer proxy;

    public RakNetPingHandler(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RakPing rakPing) throws Exception {
        ProxyConfig config = this.proxy.getConfiguration();

        ProxyPingEvent event = new ProxyPingEvent(
                config.getMotd(),
                "WaterdogPE Proxy",
                "Survival",
                "MCPE",
                ProtocolVersion.latest().getMinecraftVersion(),
                this.proxy.getPlayerManager().getPlayers().values(),
                config.getMaxPlayerCount(),
                rakPing.getSender()
        );
        this.proxy.getEventManager().callEvent(event);

        long guid = ctx.channel().config().getOption(RakChannelOption.RAK_GUID);

        StringJoiner joiner = new StringJoiner(";");
        joiner.add("MCPE");
        joiner.add(event.getMotd().replace(";", "\\;")); // MOTD
        joiner.add(Integer.toString(ProtocolVersion.latest().getProtocol())); // Protocol id
        joiner.add(event.getVersion()); // Game version
        joiner.add(Integer.toString(event.getPlayerCount())); // Player count
        joiner.add(Integer.toString(event.getMaximumPlayerCount())); // Max players
        joiner.add(Long.toUnsignedString(guid)); // Server guid
        joiner.add(event.getSubMotd()); // Sub-motd
        joiner.add(event.getGameType()); // Game type
        joiner.add("1"); // Nintendo limited
        ctx.writeAndFlush(rakPing.reply(guid, Unpooled.wrappedBuffer(joiner.toString().getBytes(StandardCharsets.UTF_8))));
    }
}
