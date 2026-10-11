package dev.waterdog.waterdogpe.network.nethernet;

import io.netty.channel.Channel;
import org.cloudburstmc.netty.channel.nethernet.NetherNetChildChannel;
import org.cloudburstmc.netty.util.nethernet.PlayerInfo;

import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/** Carries the HTTP signaling host to the matching NetherNet player's login. */
public final class NetherNetJoinHosts {
    private final ConcurrentHashMap<IdentityKey, String> hosts = new ConcurrentHashMap<>();

    public void record(String host, PlayerInfo player) {
        String hostname = normalize(host);
        if (hostname == null || player == null) {
            return;
        }
        IdentityKey key = new IdentityKey(player);
        this.hosts.put(key, hostname);
        // Rejected or abandoned offers never create a player channel.
        CompletableFuture.delayedExecutor(60, TimeUnit.SECONDS).execute(() -> this.hosts.remove(key, hostname));
    }

    public String take(Channel channel) {
        PlayerInfo player = channel.attr(NetherNetChildChannel.PLAYER_INFO).get();
        return player == null ? null : this.hosts.remove(new IdentityKey(player));
    }

    static String normalize(String host) {
        if (host == null) {
            return null;
        }
        String hostname = host.trim().toLowerCase(Locale.ROOT);
        int colon = hostname.lastIndexOf(':');
        if (colon > 0 && colon == hostname.indexOf(':') && hostname.substring(colon + 1).matches("[0-9]{1,5}")) {
            hostname = hostname.substring(0, colon);
        }
        return hostname.matches("[a-z0-9](?:[a-z0-9.-]*[a-z0-9])?") ? hostname : null;
    }

    private record IdentityKey(PlayerInfo player) {
        @Override
        public boolean equals(Object other) {
            return other instanceof IdentityKey key && this.player == key.player;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(Objects.requireNonNull(this.player));
        }
    }
}
