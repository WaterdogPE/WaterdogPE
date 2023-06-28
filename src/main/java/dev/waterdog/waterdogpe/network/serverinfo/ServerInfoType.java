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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.protocol.common.util.Preconditions;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Predicate;

/**
 * This is the identifier class for custom ServerInfo types.
 * The aim is to allow simple comparing between the custom types. Therefore, ServerInfoType#fromString()
 * method should be used to create new ServerInfoType.
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ServerInfoType implements Comparable<ServerInfoType> {
    private static final Predicate<ServerInfoType> PREDICATE_TRUE = server -> true;
    private static final Map<String, ServerInfoType> types = new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER);

    /**
     * Vanilla Minecraft: Bedrock connection utilizing RakNet
     */
    public static final ServerInfoType BEDROCK = ServerInfoType.builder()
            .identifier("bedrock")
            .serverInfoFactory(BedrockServerInfo::new)
            .register();

    private final String identifier;
    private final ServerInfoFactory serverInfoFactory;

    public static ServerInfoType fromString(String string) {
        Preconditions.checkNotNull(string, "ServerInfoType name can not be null");
        Preconditions.checkArgument(!string.isEmpty(), "ServerInfoType name can not be empty");
        return types.get(string);
    }

    public static Collection<ServerInfoType> values() {
        return Collections.unmodifiableCollection(types.values());
    }

    @Override
    public String toString() {
        return this.identifier;
    }

    @Override
    public int compareTo(ServerInfoType serverInfoType) {
        return this.identifier.compareTo(serverInfoType.getIdentifier());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String identifier;
        private ServerInfoFactory serverInfoFactory;

        public Builder identifier(String name) {
            this.identifier = name;
            return this;
        }

        public Builder serverInfoFactory(ServerInfoFactory factory) {
            this.serverInfoFactory = factory;
            return this;
        }

        public ServerInfoType register() {
            Preconditions.checkNotNull(this.identifier, "identifier");
            Preconditions.checkNotNull(this.serverInfoFactory, "server info factory");
            Preconditions.checkArgument(!types.containsKey(this.identifier), "ServerInfoType " + this.identifier + " already exists");

            ServerInfoType type = new ServerInfoType(this.identifier, this.serverInfoFactory);
            types.put(this.identifier, type);
            return type;
        }
    }

    public interface ServerInfoFactory {
        ServerInfo createServerInfo(String serverName, InetSocketAddress address, InetSocketAddress publicAddress);
    }
}
