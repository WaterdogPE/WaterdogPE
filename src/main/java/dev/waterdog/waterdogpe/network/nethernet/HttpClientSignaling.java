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

package dev.waterdog.waterdogpe.network.nethernet;

import org.cloudburstmc.netty.channel.nethernet.NetherNetConstants;
import org.cloudburstmc.netty.channel.nethernet.signaling.NetherNetClientSignaling;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.ScheduledFuture;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.extern.log4j.Log4j2;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.security.KeyPair;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * The client half of HTTP signaling, used when the proxy connects out to a NetherNet server.
 * <p>
 * The transport's signaling bus is built for trickle ICE while HTTP signaling is a single request
 * and response, so the offer is held back until candidate gathering goes quiet, posted once with
 * every candidate folded in, and the answer handed straight back to the channel.
 */
@Log4j2
public class HttpClientSignaling implements NetherNetClientSignaling {

    private static final long CANDIDATE_QUIET_MILLIS = 700;
    private static final long GATHER_CAP_MILLIS = 5_000;
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(10);

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    private final String localNetworkId = Long.toUnsignedString(ThreadLocalRandom.current().nextLong());
    private final List<String> candidates = new ObjectArrayList<>();
    private final EventLoop eventLoop;
    private final InetSocketAddress address;
    private final Identity identity;

    private volatile SignalHandler handler;
    private volatile NotFoundHandler notFound;
    private volatile boolean closed;

    private String offer;
    private long connectionId;
    private ScheduledFuture<?> quiet;
    private ScheduledFuture<?> deadline;
    private boolean sent;

    public HttpClientSignaling(EventLoop eventLoop, InetSocketAddress address, Identity identity) {
        this.eventLoop = eventLoop;
        this.address = address;
        this.identity = identity;
    }

    /**
     * What the proxy asserts about itself and the player it is connecting on behalf of. Servers
     * that require an assertion reject an offer without one.
     */
    public record Identity(KeyPair keyPair, String xuid, String name, String domain) {
    }

    /**
     * The capability probe. A non 2xx here is how a server says it does not speak NetherNet, which
     * is what drives the fallback to RakNet.
     */
    public static CompletableFuture<Boolean> probe(InetSocketAddress address) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl(address) + "/v1/join"))
                .timeout(HTTP_TIMEOUT)
                .header("Accept", "application/json")
                .GET()
                .build();
        return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> response.statusCode() / 100 == 2)
                .exceptionally(error -> false);
    }

    @Override
    public CompletableFuture<List<IceServerInfo>> connect(SocketAddress remoteAddress) {
        // No STUN or TURN: with full ICE every configured server has to be reached before the
        // offer can be sent, which the onboarding guide warns against.
        return CompletableFuture.completedFuture(List.of());
    }

    @Override
    public void sendSignal(String targetNetworkId, String data) {
        String[] parts = data.split(" ", 3);
        if (parts.length < 3) {
            return;
        }
        long id;
        try {
            id = Long.parseUnsignedLong(parts[1]);
        } catch (NumberFormatException e) {
            return;
        }
        String type = parts[0];
        String payload = parts[2];
        this.eventLoop.execute(() -> this.onSignal(id, type, payload));
    }

    private void onSignal(long id, String type, String payload) {
        if (this.closed || this.sent) {
            return;
        }
        switch (type) {
            case NetherNetConstants.RTC_NEGOTIATION_CONNECT_REQUEST -> {
                this.connectionId = id;
                this.offer = payload;
                this.deadline = this.eventLoop.schedule(this::post, GATHER_CAP_MILLIS, TimeUnit.MILLISECONDS);
                this.rearm();
            }
            case NetherNetConstants.RTC_NEGOTIATION_CANDIDATE_ADD -> {
                this.candidates.add(payload);
                this.rearm();
            }
            default -> log.debug("Unhandled outbound NetherNet signal {}", type);
        }
    }

    /** Silence is the only completion cue this bus offers, there is no gathering complete signal. */
    private void rearm() {
        if (this.offer == null) {
            return;
        }
        if (this.quiet != null) {
            this.quiet.cancel(false);
        }
        this.quiet = this.eventLoop.schedule(this::post, CANDIDATE_QUIET_MILLIS, TimeUnit.MILLISECONDS);
    }

    private void post() {
        if (this.closed || this.sent || this.offer == null) {
            return;
        }
        this.sent = true;
        this.cancelTimers();

        String body = SdpUtil.withCandidates(this.offer, this.candidates);
        if (this.identity != null) {
            try {
                // Signed over the finished offer, so every fingerprint it binds to is already in it.
                body = SdpUtil.withIdentity(body, ClientAssertionFactory.create(body, this.identity.keyPair(),
                        this.identity.xuid(), this.identity.name(), this.identity.domain()));
            } catch (Exception e) {
                this.fail("unable to sign the identity assertion: " + e);
                return;
            }
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl(this.address) + "/v1/join/" + this.localNetworkId))
                .timeout(HTTP_TIMEOUT)
                .header("Content-Type", "application/sdp")
                .header("Accept", "application/sdp")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .whenCompleteAsync((response, error) -> this.onAnswer(response, error), this.eventLoop);
    }

    private void onAnswer(HttpResponse<String> response, Throwable error) {
        if (this.closed) {
            return;
        }
        if (error != null) {
            this.fail("signaling request failed: " + error.getMessage());
            return;
        }
        if (response.statusCode() / 100 != 2) {
            this.fail("signaling returned HTTP " + response.statusCode());
            return;
        }

        String answer = response.body();
        // A rejection can arrive as a 2xx with a short body instead of an SDP, so the shape has to
        // be checked rather than the status alone.
        if (answer == null || !answer.startsWith("v=")) {
            this.fail("signaling returned a 2xx that is not an SDP answer: "
                    + (answer == null ? "empty" : answer.strip()));
            return;
        }

        SignalHandler target = this.handler;
        if (target != null) {
            target.onSignal(NetherNetConstants.buildSignalConnectResponse(this.connectionId, answer));
        }
    }

    private void fail(String reason) {
        // Worth a warning rather than a debug line: reaching here means signaling answered and
        // still did not produce a usable connection, which the probe cannot predict.
        log.warn("[{}] NetherNet signaling failed: {}", this.address, reason);
        NotFoundHandler target = this.notFound;
        if (target != null) {
            target.onNotFound(reason);
        }
    }

    private static String baseUrl(InetSocketAddress address) {
        InetAddress host = address.getAddress();
        String literal = host == null ? address.getHostString() : host.getHostAddress();
        // Already resolved, so the URL never triggers another lookup. IPv6 needs brackets.
        return "http://" + (literal.indexOf(':') >= 0 ? "[" + literal + "]" : literal) + ":" + address.getPort();
    }

    private void cancelTimers() {
        if (this.quiet != null) {
            this.quiet.cancel(false);
        }
        if (this.deadline != null) {
            this.deadline.cancel(false);
        }
    }

    @Override
    public void setSignalHandler(long connectionId, SignalHandler handler) {
        this.handler = handler;
    }

    @Override
    public void removeSignalHandler(long connectionId) {
        this.handler = null;
    }

    @Override
    public void setNotFoundHandler(NotFoundHandler handler) {
        this.notFound = handler;
    }

    @Override
    public String getLocalNetworkId() {
        return this.localNetworkId;
    }


    @Override
    public boolean isActive() {
        return !this.closed;
    }

    @Override
    public void close() {
        this.closed = true;
        if (this.eventLoop.inEventLoop()) {
            this.cancelTimers();
        } else {
            this.eventLoop.execute(this::cancelTimers);
        }
    }
}
