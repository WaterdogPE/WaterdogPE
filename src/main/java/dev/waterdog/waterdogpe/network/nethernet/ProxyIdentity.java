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
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import org.cloudburstmc.netty.util.nethernet.OperatorIdentity;
import org.jose4j.lang.JoseException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;

/**
 * The proxy's NetherNet identity, which identifies it to connecting clients, loaded once and lazily.
 * A connection to a downstream server presents one per player instead, see {@link #forPlayer}.
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

    /**
     * The identity a connection to a downstream server presents for {@code player}, under this proxy's name. It
     * carries the key their downstream login is signed with, as BDS refuses a login whose key the connection does
     * not present. The token names the player and expires, so derive one per connection.
     */
    public static OperatorIdentity forPlayer(ProxiedPlayer player) throws JoseException {
        return forPlayer(player.getLoginData().getKeyPair(), domain(player.getProxy()), player.getXuid(),
                player.getName());
    }

    static OperatorIdentity forPlayer(KeyPair loginKey, String domain, String xuid, String name) throws JoseException {
        return new OperatorIdentity(loginKey.getPrivate(), loginKey.getPublic(), null, domain).forPlayer(xuid, name);
    }
}
