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

package dev.waterdog.waterdogpe.network.serverinfo;

import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.network.connection.client.ClientConnection;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import io.netty.util.concurrent.Future;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSets;
import lombok.Getter;
import lombok.ToString;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Base informative class for servers.
 * Every server registered to the Proxy has one instance of this class, holding its name, and it's address (ip&port)
 * Also holds a list of all ProxiedPlayers connected.
 */
@ToString(exclude = {"players"})
public abstract class ServerInfo {

    /**
     * How long a resolved downstream address is reused before a background re-resolve is triggered.
     * A miss only schedules an async refresh; callers always get the last-known-good address immediately.
     */
    private static final long DNS_CACHE_TTL_MS = TimeUnit.SECONDS.toMillis(30);

    /**
     * Single daemon thread that performs the (blocking) JDK name resolution OFF the Netty event loops.
     * Historically {@code connect()} was handed an unresolved InetSocketAddress, so Netty resolved it
     * inline on a worker event loop via blocking InetAddress.getByName, a slow/hung DNS lookup there
     * froze every player sharing that loop. All resolution now happens on this thread instead.
     */
    private static final ExecutorService DNS_RESOLVER = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "ServerInfo DNS Resolver");
        thread.setDaemon(true);
        return thread;
    });

    @Getter
    private final String serverName;
    @Getter
    private final InetSocketAddress address;
    @Getter
    private final InetSocketAddress publicAddress;

    // Last successfully resolved connect target. Never null: seeded with the configured address so behavior
    // never regresses below the old "resolve inline" path even if DNS is down at startup.
    private volatile InetSocketAddress resolvedAddress;
    private volatile long resolvedAt;
    private final AtomicBoolean resolving = new AtomicBoolean(false);

    private final Set<ClientConnection> connections = ObjectSets.synchronize(new ObjectOpenHashSet<>());
    private final Set<ProxiedPlayer> players = ObjectSets.synchronize(new ObjectOpenHashSet<>());

    public ServerInfo(String serverName, InetSocketAddress address, InetSocketAddress publicAddress) {
        this.serverName = serverName;
        this.address = address;
        if (publicAddress == null) {
            publicAddress = address;
        }
        this.publicAddress = publicAddress;

        // Seed the cache. Done synchronously here (constructed at startup/config-load, never on an event loop)
        // so the very first connect already has a resolved address and Netty never resolves inline.
        this.resolvedAddress = address;
        this.refreshResolvedAddress();
    }

    /**
     * Returns a resolved (non-blocking to use) connect target for this server. Always returns immediately
     * with the last-known-good address; if the cache is older than {@link #DNS_CACHE_TTL_MS} a re-resolve is
     * scheduled on the {@link #DNS_RESOLVER} thread so the event loop never performs DNS itself.
     */
    public InetSocketAddress getResolvedAddress() {
        if (System.currentTimeMillis() - this.resolvedAt > DNS_CACHE_TTL_MS && this.resolving.compareAndSet(false, true)) {
            DNS_RESOLVER.execute(() -> {
                try {
                    this.refreshResolvedAddress();
                } finally {
                    this.resolving.set(false);
                }
            });
        }
        return this.resolvedAddress;
    }

    protected void refreshResolvedAddress() {
        InetSocketAddress configured = this.address;
        try {
            // getByName on an IP literal is purely local (no network), so IP-configured servers resolve cheaply
            // and still short-circuit Netty's resolver because the result is a resolved InetSocketAddress.
            InetAddress resolved = InetAddress.getByName(configured.getHostString());
            this.resolvedAddress = new InetSocketAddress(resolved, configured.getPort());
        } catch (UnknownHostException e) {
            // Keep the last-known-good (or the raw configured) address; a transient DNS failure must not break
            // connects. The next access past the TTL will retry.
            if (ProxyServer.getInstance() != null) {
                ProxyServer.getInstance().getLogger().debug("Failed to resolve address for server {} ({}): {}",
                        this.serverName, configured.getHostString(), e.getMessage());
            }
        } finally {
            this.resolvedAt = System.currentTimeMillis();
        }
    }

    public abstract ServerInfoType getServerType();
    public abstract Future<ClientConnection> createConnection(ProxiedPlayer player);

    public boolean matchAddress(String address, int port) {
        InetAddress inetAddress = this.publicAddress.getAddress();
        boolean addressMatch;
        if (inetAddress == null) {
            addressMatch = (this.publicAddress.getHostString().equals(address) || this.publicAddress.getHostName().equals(address));
        } else {
            addressMatch = (inetAddress.getHostAddress().equals(address) || inetAddress.getHostName().equals(address));
        }
        return addressMatch && this.publicAddress.getPort() == port;
    }

    public void addConnection(ClientConnection connection) {
        if (connection != null && connection.isConnected()) {
            this.players.add(connection.getPlayer());
            this.connections.add(connection);
        }
    }

    public void removeConnection(ClientConnection connection) {
        if (connection != null) {
            this.connections.remove(connection);
            this.players.remove(connection.getPlayer());
        }
    }

    public Set<ProxiedPlayer> getPlayers() {
        return Collections.unmodifiableSet(this.players);
    }

    public Set<ClientConnection> getConnections() {
        return Collections.unmodifiableSet(this.connections);
    }

}
