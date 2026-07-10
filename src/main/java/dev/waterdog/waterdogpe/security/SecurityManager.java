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
import io.netty.handler.codec.DecoderException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Log4j2
public class SecurityManager {
    // Malformed-packet flood visibility: one summary line at most this often, and only once the count in
    // a window crosses this threshold, so incidental junk from a normal client does not raise a false alarm.
    private static final long FLOOD_REPORT_INTERVAL_MS = 5000;
    private static final long FLOOD_REPORT_THRESHOLD = 50;

    private final ProxyServer proxy;
    @Getter
    private final ConnectionThrottle connectionThrottle;
    @Getter
    private final ConnectionThrottle loginThrottle;

    private final Map<InetAddress, Long> blockedConnections = new ConcurrentHashMap<>();
    private final AtomicLong malformedSinceReport = new AtomicLong();
    private final AtomicLong lastFloodReport = new AtomicLong();

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
                log.debug("Unblocked address {}", entry.getKey());
            }
        }
    }

    public void blockAddress(InetAddress address, long time, TimeUnit unit) {
        // Bound the table so a flood of many (possibly spoofed) source addresses can not exhaust the heap.
        // Once full we stop tracking new offenders until entries expire; throttling and edge mitigation cover the rest.
        int max = this.proxy.getNetworkSettings().maxBlockedAddresses();
        if (max > 0 && this.blockedConnections.size() >= max && !this.blockedConnections.containsKey(address)) {
            return;
        }
        long millis = unit.toMillis(time);
        this.blockedConnections.put(address, System.currentTimeMillis() + millis);
        log.debug("Connection from {} is blocked for {}ms", address, millis);
    }

    public void unblockAddress(InetAddress address) {
        if (this.blockedConnections.remove(address) != null) {
            log.debug("Unblocked address {}", address);
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

    public void onConnectionError(SocketAddress address, Throwable cause) {
        if (cause != null) {
            // Malformed/oversized packets from the network are untrusted input and expected under a flood.
            // Logging a full stack trace for each one is the amplification vector that turns a cheap packet
            // flood into an outage. Keep the per-packet detail at debug and surface a rate-limited summary
            // instead, so a flood stays visible without the volume; reserve error+stack for real faults.
            if (isNetworkInputError(cause)) {
                log.debug("{} sent a packet that could not be decoded: {}", address, cause.toString());
                this.tallyMalformed();
            } else {
                log.error("{} Exception caught in bedrock connection", address, cause);
            }
        }

        int timeout = this.proxy.getNetworkSettings().errorTimeout();
        if (timeout == 0) {
            return;
        }

        if (address instanceof InetSocketAddress inet) {
            this.blockAddress(inet.getAddress(), timeout, TimeUnit.SECONDS);
        }
    }

    /**
     * Handles an exception on the parent (bind) channel, which has no per-connection address to block.
     * Malformed inbound packets are folded into the flood summary; genuine faults are logged in full.
     */
    public void onParentChannelError(Throwable cause) {
        if (isNetworkInputError(cause)) {
            log.debug("Parent channel dropped a packet that could not be decoded: {}", cause.toString());
            this.tallyMalformed();
        } else {
            log.error("Parent channel has thrown an exception", cause);
        }
    }

    /**
     * Records a dropped malformed packet and, at most once per {@link #FLOOD_REPORT_INTERVAL_MS} and only
     * once the count in a window crosses {@link #FLOOD_REPORT_THRESHOLD}, logs a single visible summary so
     * an ongoing flood is obvious at the default log level without logging every packet.
     */
    private void tallyMalformed() {
        this.malformedSinceReport.incrementAndGet();
        long now = System.currentTimeMillis();
        long last = this.lastFloodReport.get();
        if (now - last >= FLOOD_REPORT_INTERVAL_MS && this.lastFloodReport.compareAndSet(last, now)) {
            long count = this.malformedSinceReport.getAndSet(0);
            if (count >= FLOOD_REPORT_THRESHOLD) {
                log.warn("Dropping malformed packets from clients ({} in the last ~{}s) — possible packet flood; enable DEBUG for per-packet detail",
                        count, FLOOD_REPORT_INTERVAL_MS / 1000);
            }
        }
    }

    /**
     * Whether the throwable originates from decoding untrusted network input (a malformed or truncated
     * packet), as opposed to a genuine internal error. Such errors are expected and must not be logged
     * per-packet with a stack trace.
     */
    private static boolean isNetworkInputError(Throwable cause) {
        for (Throwable t = cause; t != null; t = (t.getCause() == t ? null : t.getCause())) {
            if (t instanceof DecoderException || t instanceof IndexOutOfBoundsException) {
                return true;
            }
        }
        return false;
    }
}
