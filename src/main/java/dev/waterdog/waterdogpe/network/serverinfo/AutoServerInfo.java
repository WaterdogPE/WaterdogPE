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

package dev.waterdog.waterdogpe.network.serverinfo;

import dev.waterdog.waterdogpe.network.connection.client.ClientConnection;
import dev.waterdog.waterdogpe.network.nethernet.HttpClientSignaling;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import lombok.extern.log4j.Log4j2;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

/**
 * A downstream server whose transport is discovered rather than configured.
 * <p>
 * NetherNet is tried first and RakNet is the fallback. The outcome is remembered so that every
 * join does not pay for a probe, but never permanently: the memory expires on a timer, and a failure on the remembered
 * transport immediately retries the other one and adopts it. A server restarting into a different
 * transport is therefore picked up on the next join rather than staying wrong until the proxy
 * restarts.
 */
@Log4j2
public class AutoServerInfo extends ServerInfo {

    private enum Transport {
        NETHERNET,
        RAKNET;

        Transport other() {
            return this == NETHERNET ? RAKNET : NETHERNET;
        }
    }

    private volatile Transport remembered;
    private volatile long rememberedAt;

    public AutoServerInfo(String serverName, InetSocketAddress address, InetSocketAddress publicAddress) {
        super(serverName, address, publicAddress);
    }

    @Override
    public ServerInfoType getServerType() {
        return ServerInfoType.BEDROCK;
    }

    @Override
    public Future<ClientConnection> createConnection(ProxiedPlayer player) {
        EventLoop eventLoop = player.getProxy().getWorkerEventLoopGroup().next();
        Promise<ClientConnection> promise = eventLoop.newPromise();

        Transport known = this.recall(player.getProxy().getNetherNetSettings().getTransportMemory());
        if (known != null) {
            this.attempt(player, promise, known, true);
            return promise;
        }

        HttpClientSignaling.probe(this.getResolvedAddress()).whenCompleteAsync((supported, error) -> {
            Transport choice = Boolean.TRUE.equals(supported) ? Transport.NETHERNET : Transport.RAKNET;
            log.debug("[{}] Probed as {}", this.getServerName(), choice);
            this.attempt(player, promise, choice, true);
        }, eventLoop);
        return promise;
    }

    private void attempt(ProxiedPlayer player, Promise<ClientConnection> promise, Transport transport, boolean mayRetry) {
        BiFunction<ProxiedPlayer, ServerInfo, Future<ClientConnection>> connector = transport == Transport.NETHERNET
                ? NetherNetServerInfo::connect
                : BedrockServerInfo::connect;

        connector.apply(player, this).addListener(future -> {
            if (future.isSuccess()) {
                this.remember(transport);
                promise.trySuccess((ClientConnection) future.getNow());
                return;
            }

            if (!mayRetry || promise.isDone()) {
                log.debug("[{}] {} failed and there is nothing left to try", this.getServerName(), transport, future.cause());
                this.forget();
                promise.tryFailure(future.cause());
                return;
            }

            // The remembered transport stopped working, so adopt the other one rather than
            // failing the join and staying wrong until the memory expires.
            Transport fallback = transport.other();
            log.info("[{}] {} failed, falling back to {}", this.getServerName(), transport, fallback, future.cause());
            this.attempt(player, promise, fallback, false);
        });
    }

    private Transport recall(int memorySeconds) {
        Transport known = this.remembered;
        if (known == null || memorySeconds <= 0) {
            return null;
        }
        long age = System.currentTimeMillis() - this.rememberedAt;
        return age < TimeUnit.SECONDS.toMillis(memorySeconds) ? known : null;
    }

    private void remember(Transport transport) {
        this.remembered = transport;
        this.rememberedAt = System.currentTimeMillis();
    }

    private void forget() {
        this.remembered = null;
    }
}
