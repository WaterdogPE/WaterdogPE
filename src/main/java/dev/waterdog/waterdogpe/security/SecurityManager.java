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

package dev.waterdog.waterdogpe.security;

import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.network.NetworkMetrics;
import dev.waterdog.waterdogpe.network.NetworkMetrics.ConnectionError;
import dev.waterdog.waterdogpe.network.NetworkMetrics.LoginFailure;
import dev.waterdog.waterdogpe.network.protocol.user.HandshakeEntry;
import dev.waterdog.waterdogpe.utils.config.proxy.NetworkSettings;
import io.netty.handler.codec.DecoderException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

@Log4j2
public class SecurityManager {
    private final ProxyServer proxy;
    @Getter
    private final ConnectionThrottle connectionThrottle;
    @Getter
    private final ConnectionThrottle loginThrottle;

    private final Map<InetAddress, Long> blockedConnections = new ConcurrentHashMap<>();

    @Getter
    @Setter
    private SecurityListener listener;

    public SecurityManager(ProxyServer proxy) {
        this.proxy = proxy;

        NetworkSettings settings = proxy.getNetworkSettings();
        int time = settings.getConnectionThrottleTime();
        if (time < 50) {
            throw new IllegalArgumentException("For performance reasons connection throttle time should be 50ms or more.");
        }

        this.connectionThrottle = settings.getConnectionThrottle() > 0 ? new ConnectionThrottle(settings.getConnectionThrottle(), time) : null;
        this.loginThrottle = settings.getConnectionThrottle() > 0 ? new ConnectionThrottle(settings.getLoginThrottle(), time) : null;
        proxy.getScheduler().scheduleRepeating(this::onBlockedTick, 1);
    }

    private void onBlockedTick() {
        long currTime = System.currentTimeMillis();

        Iterator<Map.Entry<InetAddress, Long>> iterator = this.blockedConnections.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<InetAddress, Long> entry = iterator.next();
            if (entry.getValue() != 0 && currTime > entry.getValue()) {
                iterator.remove();
                this.proxy.getLogger().info("Unblocked address {}", entry.getKey());
            }
        }
    }

    public void blockAddress(InetAddress address, long time, TimeUnit unit) {
        long millis = unit.toMillis(time);
        this.blockedConnections.put(address, System.currentTimeMillis() + millis);
        this.proxy.getLogger().info("Connection from {} is blocked for {}ms", address, millis);
    }

    public void unblockAddress(InetAddress address) {
        if (this.blockedConnections.remove(address) != null) {
            this.proxy.getLogger().info("Unblocked address {}", address);
        }
    }

    public boolean isAddressBlocked(InetAddress address) {
        return this.blockedConnections.containsKey(address);
    }

    public boolean onConnectionCreated(SocketAddress address) {
        return this.admit(address, this.connectionThrottle, LoginFailure.CONNECTION_THROTTLED,
                listener -> listener.onConnectionCreated(address));
    }

    public boolean onLoginAttempt(SocketAddress address) {
        return this.admit(address, this.loginThrottle, LoginFailure.LOGIN_THROTTLED,
                listener -> listener.onLoginAttempt(address));
    }

    private boolean admit(SocketAddress address, ConnectionThrottle throttle, LoginFailure throttled,
                          Predicate<SecurityListener> allowed) {
        boolean success = true;
        if (throttle != null && address instanceof InetSocketAddress addr) {
            success = throttle.throttle(addr.getAddress());
            if (!success) {
                this.loginFailed(throttled);
                if (this.listener != null) {
                    this.listener.onThrottleReached(addr.getAddress(), throttle);
                }
            }
        }

        if (this.listener != null && !allowed.test(this.listener)) {
            if (success) {
                this.loginFailed(LoginFailure.LISTENER);
            }
            success = false;
        }
        return success;
    }

    public String onLoginFailed(SocketAddress address, HandshakeEntry handshakeEntry, Throwable throwable, String reason) {
        this.loginFailed(this.loginFailure(handshakeEntry, throwable));
        if (this.listener != null) {
            return this.listener.onLoginFailed(address, handshakeEntry, throwable, reason);
        }
        // TODO: probably throttle this as well?
        return reason;
    }

    /**
     * LoginUpstreamHandler fails a login that threw, one whose chain the auth service did not sign, which
     * only online mode refuses, and otherwise one whose key the transport identity does not hold.
     */
    private LoginFailure loginFailure(HandshakeEntry entry, Throwable throwable) {
        if (throwable != null || entry == null) {
            return LoginFailure.ERROR;
        }
        if (!entry.isXboxAuthed() && this.proxy.getConfiguration().isOnlineMode()) {
            return LoginFailure.XBOX_AUTH;
        }
        return LoginFailure.IDENTITY_KEY;
    }

    private void loginFailed(LoginFailure reason) {
        NetworkMetrics metrics = this.proxy.getNetworkMetrics();
        if (metrics != null) {
            metrics.loginFailed(reason);
        }
    }

    /**
     * Called when a client did not complete the login handshake in time.
     */
    public void onLoginTimeout(SocketAddress address) {
        this.onConnectionError(address, null, ConnectionError.TIMEOUT);
    }

    public void onConnectionError(SocketAddress address, Throwable cause) {
        this.onConnectionError(address, cause, connectionError(cause));
    }

    private static ConnectionError connectionError(Throwable cause) {
        if (cause == null) {
            return ConnectionError.PROTOCOL;
        }
        if (cause instanceof DecoderException) {
            return ConnectionError.DECODE;
        }
        return cause instanceof IOException ? ConnectionError.IO : ConnectionError.OTHER;
    }

    private void onConnectionError(SocketAddress address, Throwable cause, ConnectionError kind) {
        if (cause != null) {
            log.error("{} Exception caught in bedrock connection", address, cause);
        }

        NetworkMetrics metrics = this.proxy.getNetworkMetrics();
        if (metrics != null) {
            metrics.connectionError(kind);
        }

        int timeout = this.proxy.getNetworkSettings().errorTimeout();
        if (timeout == 0) {
            return;
        }

        if (address instanceof InetSocketAddress inet) {
            this.proxy.getSecurityManager().blockAddress(inet.getAddress(), timeout, TimeUnit.SECONDS);
        }
    }
}
