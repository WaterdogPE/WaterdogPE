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

package dev.waterdog.waterdogpe.network.connection.codec.server;

import dev.waterdog.waterdogpe.ProxyServer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ServerErrorHandler {
    public static final String NAME = "error-handler";

    public static class Parent extends ChannelInboundHandlerAdapter{
        private final ProxyServer proxy;

        public Parent(ProxyServer proxy) {
            this.proxy = proxy;
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            log.error("Parent channel has thrown an exception", cause);
        }
    }

    public static class Child extends ChannelInboundHandlerAdapter {
        private final ProxyServer proxy;

        public Child(ProxyServer proxy) {
            this.proxy = proxy;
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            this.proxy.getSecurityManager().onConnectionError(ctx.channel().remoteAddress(), cause);
        }
    }
}
