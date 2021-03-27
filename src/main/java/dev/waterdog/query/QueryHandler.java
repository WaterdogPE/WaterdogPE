/*
 * Copyright 2021 WaterdogTEAM
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

package dev.waterdog.query;

import dev.waterdog.ProxyServer;
import dev.waterdog.event.defaults.ProxyQueryEvent;
import dev.waterdog.player.ProxiedPlayer;
import dev.waterdog.utils.ProxyConfig;
import dev.waterdog.utils.types.TranslationContainer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class QueryHandler {

    public static final byte[] QUERY_SIGNATURE = new byte[]{(byte) 0xFE, (byte) 0xFD};
    public static final byte[] LONG_RESPONSE_PADDING_TOP = new byte[]{115, 112, 108, 105, 116, 110, 117, 109, 0, -128, 0};
    public static final byte[] LONG_RESPONSE_PADDING_BOTTOM = new byte[]{1, 112, 108, 97, 121, 101, 114, 95, 0, 0};

    public static final short PACKET_HANDSHAKE = 0x09;
    public static final short PACKET_STATISTICS = 0x00;
    private static final String GAME_ID = "MINECRAFTPE";

    private final ProxyServer proxy;
    private final InetSocketAddress bindAddress;

    private final Object2ObjectMap<InetAddress, QuerySession> querySessions = new Object2ObjectOpenHashMap<>();

    public QueryHandler(ProxyServer proxy, InetSocketAddress bindAddress) {
        this.proxy = proxy;
        this.bindAddress = bindAddress;
        this.proxy.getLogger().info(new TranslationContainer("waterdog.query.start", bindAddress.toString()).getTranslated());
    }

    private void writeInt(ByteBuf buf, int i) {
        this.writeString(buf, Integer.toString(i));
    }

    private void writeString(ByteBuf buf, String string) {
        for (char c : string.toCharArray()) {
            buf.writeByte(c);
        }
        buf.writeByte(0);
    }

    public void onQuery(InetSocketAddress address, ByteBuf packet, ChannelHandlerContext ctx) {
        short packetId = packet.readUnsignedByte();
        int sessionId = packet.readInt();

        if (packetId == PACKET_HANDSHAKE) {
            ByteBuf reply = ctx.alloc().ioBuffer(10);
            reply.writeByte(PACKET_HANDSHAKE);
            reply.writeInt(sessionId);

            int token = ThreadLocalRandom.current().nextInt();
            this.querySessions.put(address.getAddress(), new QuerySession(token, System.currentTimeMillis()));
            this.writeInt(reply, token);
            this.proxy.getBedrockServer().getRakNet().send(address, reply);
            return;
        }

        if (packetId == PACKET_STATISTICS && packet.isReadable(4)) {
            QuerySession session = this.querySessions.remove(address.getAddress());
            int token = packet.readInt();
            if (session == null || session.token != token) {
                return;
            }

            ByteBuf reply = ctx.alloc().ioBuffer(64);
            reply.writeByte(PACKET_STATISTICS);
            reply.writeInt(sessionId);

            this.writeData(address, packet.readableBytes() == 8, reply);
            this.proxy.getBedrockServer().getRakNet().send(address, reply);
        }
    }

    private void writeData(InetSocketAddress address, boolean simple, ByteBuf buf) {
        ProxyConfig config = this.proxy.getConfiguration();
        ProxyQueryEvent event = new ProxyQueryEvent(
                config.getMotd(),
                "SMP",
                "MCPE",
                "",
                this.proxy.getPlayerManager().getPlayers().values(),
                config.getMaxPlayerCount(),
                "WaterdogPE",
                address
        );
        this.proxy.getEventManager().callEvent(event);

        if (simple) {
            this.writeString(buf, event.getMotd());
            this.writeString(buf, event.getGameType());
            this.writeString(buf, event.getMap());
            this.writeString(buf, Integer.toString(event.getPlayerCount()));
            this.writeString(buf, Integer.toString(event.getMaximumPlayerCount()));
            buf.writeShortLE(this.bindAddress.getPort());
            this.writeString(buf, this.bindAddress.getHostName());
            return;
        }

        Map<String, String> map = new Object2ObjectArrayMap<>();
        map.put("hostname", event.getMotd());
        map.put("gametype", event.getGameType());
        map.put("map", event.getMap());
        map.put("numplayers", Integer.toString(event.getPlayerCount()));
        map.put("maxplayers", Integer.toString(event.getMaximumPlayerCount()));
        map.put("hostport", Integer.toString(this.bindAddress.getPort()));
        map.put("hostip", this.bindAddress.getHostName());
        map.put("game_id", GAME_ID);
        map.put("version", event.getVersion());
        map.put("plugins", ""); // Do not list plugins
        map.put("whitelist", event.hasWhitelist() ? "on" : "off");

        buf.writeBytes(LONG_RESPONSE_PADDING_TOP);
        map.forEach((key, value) -> {
            this.writeString(buf, key);
            this.writeString(buf, value);
        });
        buf.writeByte(0);
        buf.writeBytes(LONG_RESPONSE_PADDING_BOTTOM);

        if (event.getPlayers().size() >= 1) {
            for (ProxiedPlayer player : event.getPlayers()) {
                this.writeString(buf, player.getName());
            }
        }
        buf.writeByte(0);
    }

    private static class QuerySession {

        public final int token;
        public final long time;

        public QuerySession(int token, long time) {
            this.token = token;
            this.time = time;
        }
    }
}
