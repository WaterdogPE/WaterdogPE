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
import dev.waterdog.waterdogpe.network.protocol.user.HandshakeEntry;
import dev.waterdog.waterdogpe.utils.config.proxy.NetworkSettings;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class SecurityManager {
    private final ProxyServer proxy;
    private final ConnectionThrottle connectionThrottle;
    private final ConnectionThrottle loginThrottle;

    private final Map<InetAddress, Long> blockedConnections = new ConcurrentHashMap<>();

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
        boolean success = true;
        if (this.connectionThrottle != null && address instanceof InetSocketAddress addr) {
            success = this.connectionThrottle.throttle(addr.getAddress());
            if (!success && this.listener != null) {
                this.listener.onThrottleReached(addr.getAddress(), this.connectionThrottle);
            }
        }

        if (this.listener != null && !this.listener.onConnectionCreated(address)) {
            success = false;
        }
        return success;
    }

    public boolean onLoginAttempt(SocketAddress address) {
        boolean success = true;
        if (this.loginThrottle != null && address instanceof InetSocketAddress addr) {
            success = this.loginThrottle.throttle(addr.getAddress());
            if (!success && this.listener != null) {
                this.listener.onThrottleReached(addr.getAddress(), this.loginThrottle);
            }
        }

        if (this.listener != null && !this.listener.onLoginAttempt(address)) {
            success = false;
        }
        return success;
    }

    public String onLoginFailed(SocketAddress address, HandshakeEntry handshakeEntry, Throwable throwable, String reason) {
        if (this.listener != null) {
            return this.listener.onLoginFailed(address, handshakeEntry, throwable, reason);
        }
        // TODO: probably throttle this as well?
        return reason;
    }

    public void setListener(SecurityListener listener) {
        this.listener = listener;
    }

    public SecurityListener getListener() {
        return this.listener;
    }

    public ConnectionThrottle getConnectionThrottle() {
        return this.connectionThrottle;
    }

    public ConnectionThrottle getLoginThrottle() {
        return this.loginThrottle;
    }
}
