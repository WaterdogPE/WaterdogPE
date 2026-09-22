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
import org.cloudburstmc.netty.util.nethernet.OperatorIdentity;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The proxy's NetherNet identity, used both to identify it to connecting clients and to sign the
 * assertions it presents to downstream servers.
 * <p>
 * Loaded once and lazily, because the downstream leg needs it even when the proxy accepts no
 * NetherNet connections of its own.
 */
public final class ProxyIdentity {

    private static volatile OperatorIdentity identity;

    private ProxyIdentity() {
    }

    /**
     * The identity, generated on the first start if it is missing.
     */
    public static OperatorIdentity identity(ProxyServer proxy) throws Exception {
        OperatorIdentity current = identity;
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

    private static OperatorIdentity load(ProxyServer proxy) throws Exception {
        // An absolute path resolves to itself, so a mounted key works
        Path file = proxy.getDataPath().resolve(proxy.getNetherNetSettings().getIdentityFile());

        boolean existed = Files.isRegularFile(file);
        OperatorIdentity loaded = OperatorIdentity.fromPemOrCreate(file.toFile(), domain(proxy));
        if (!existed) {
            proxy.getLogger().info("Generated a NetherNet identity at {}. Share this file across a fleet "
                    + "to be trusted as one operator, and keep it, replacing it re-prompts every player", file);
        }
        return loaded;
    }

    /**
     * The name the identity carries, as the assertion's provider and the token issuer. Clients pin
     * the key and nothing displays this today, so it is the listener name rather than the MOTD,
     * which plugins rewrite per ping.
     */
    public static String domain(ProxyServer proxy) {
        return Color.clean(proxy.getConfiguration().getName());
    }
}
