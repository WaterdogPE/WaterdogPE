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

import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.logger.Color;
import org.cloudburstmc.netty.util.nethernet.ServerIdentity;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;

/**
 * The proxy's NetherNet identity, used both to identify it to connecting clients and to sign the
 * assertions it presents to downstream servers.
 * <p>
 * Loaded once and lazily, because the downstream leg needs it even when the proxy accepts no
 * NetherNet connections of its own.
 */
public final class ProxyIdentity {

    private static volatile ServerIdentity identity;

    private ProxyIdentity() {
    }

    /**
     * The identity, generated on the first start if it is missing.
     */
    public static ServerIdentity identity(ProxyServer proxy) throws Exception {
        ServerIdentity current = identity;
        if (current != null) {
            return current;
        }

        synchronized (ProxyIdentity.class) {
            if (identity == null) {
                identity = load(proxy);
            }
            return identity;
        }
    }

    /**
     * The keypair behind {@link #identity}, for the assertions the proxy signs as a client.
     */
    public static KeyPair keyPair(ProxyServer proxy) throws Exception {
        return identity(proxy).keyPair();
    }

    private static ServerIdentity load(ProxyServer proxy) throws Exception {
        Path file = proxy.getDataPath().resolve(proxy.getNetherNetSettings().getIdentityFile());

        boolean existed = Files.isRegularFile(file);
        ServerIdentity loaded = ServerIdentity.fromPemOrCreate(file.toFile(), domain(proxy));
        if (!existed) {
            proxy.getLogger().info("Generated a NetherNet identity at {}. Share this file across a fleet "
                    + "to be trusted as one operator, and keep it, replacing it re-prompts every player", file);
        }
        return loaded;
    }

    /**
     * The operator name shown to players in the first use prompt. It is display text only, so it
     * can change at any time without replacing the key and re-prompting anyone.
     * <p>
     * Falls back to the listener name rather than the MOTD, which plugins rewrite per ping.
     */
    public static String domain(ProxyServer proxy) {
        String domain = proxy.getNetherNetSettings().getIdentityDomain();
        if (domain != null && !domain.isBlank()) {
            return domain;
        }
        return Color.clean(proxy.getConfiguration().getName());
    }
}
