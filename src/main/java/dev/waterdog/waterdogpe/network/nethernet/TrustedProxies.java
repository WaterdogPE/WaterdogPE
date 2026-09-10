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

import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.netty.util.nethernet.IpRangeSet;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The configured proxy list, with any {@code http} or {@code https} entry fetched and expanded into
 * the addresses it lists, one per line.
 * <p>
 * Resolved once per start and shared, because a proxy may bind several listeners and each would
 * otherwise fetch the same list again.
 */
@Log4j2
public final class TrustedProxies {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private static volatile IpRangeSet resolved;

    private TrustedProxies() {
    }

    public static synchronized IpRangeSet parse(Collection<String> entries) {
        IpRangeSet cached = resolved;
        if (cached != null) {
            return cached;
        }
        return resolved = IpRangeSet.parse(expand(entries));
    }

    /**
     * Forgets the resolved list so the next listener to start fetches it again.
     */
    public static synchronized void invalidate() {
        resolved = null;
    }

    private static List<String> expand(Collection<String> entries) {
        List<String> out = new ArrayList<>();
        for (String entry : entries) {
            String trimmed = entry == null ? "" : entry.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                out.add(trimmed);
                continue;
            }

            try {
                fetch(trimmed).lines()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                        .forEach(out::add);
            } catch (Exception e) {
                // A list that cannot be fetched must not widen trust, so it contributes nothing
                log.error("Could not fetch the trusted proxy list at {}, no addresses taken from it", trimmed, e);
            }
        }
        return out;
    }

    private static String fetch(String url) throws IOException, InterruptedException {
        HttpResponse<String> response = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build()
                .send(HttpRequest.newBuilder(URI.create(url)).timeout(TIMEOUT).GET().build(),
                        HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("returned HTTP " + response.statusCode());
        }
        return response.body();
    }
}
