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

package dev.waterdog.waterdogpe.network.connection.codec.compression;

import lombok.ToString;
import org.cloudburstmc.protocol.bedrock.data.PacketCompressionAlgorithm;
import org.cloudburstmc.protocol.common.util.Preconditions;

import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

@ToString
public class CompressionAlgorithm implements Comparable<CompressionAlgorithm> {
    private static final Map<String, CompressionAlgorithm> types = new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER);

    public static final CompressionAlgorithm ZLIB = CompressionAlgorithm.builder()
            .identifier("zlib")
            .bedrockAlgorithm(PacketCompressionAlgorithm.ZLIB)
            .register();

    public static final CompressionAlgorithm SNAPPY = CompressionAlgorithm.builder()
            .identifier("snappy")
            .bedrockAlgorithm(PacketCompressionAlgorithm.SNAPPY)
            .register();

    private final String identifier;
    private final PacketCompressionAlgorithm bedrockAlgorithm;

    private CompressionAlgorithm(String identifier, PacketCompressionAlgorithm bedrockAlgorithm) {
        this.identifier = identifier;
        this.bedrockAlgorithm = bedrockAlgorithm;
    }

    public static CompressionAlgorithm fromString(String string) {
        Preconditions.checkNotNull(string, "CompressionAlgorithm name can not be null");
        Preconditions.checkArgument(!string.isEmpty(), "CompressionAlgorithm name can not be empty");
        return types.get(string);
    }

    public static CompressionAlgorithm fromBedrockCompression(PacketCompressionAlgorithm algorithm) {
        return switch (algorithm) {
            case ZLIB -> ZLIB;
            case SNAPPY -> SNAPPY;
            default -> throw new UnsupportedOperationException("Unsupported compression " + algorithm);
        };
    }

    public String getIdentifier() {
        return this.identifier;
    }

    public PacketCompressionAlgorithm getBedrockAlgorithm() {
        return this.bedrockAlgorithm;
    }

    @Override
    public int compareTo(CompressionAlgorithm o) {
        return this.identifier.compareTo(o.identifier);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String identifier;
        private PacketCompressionAlgorithm bedrockAlgorithm;

        public Builder identifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder bedrockAlgorithm(PacketCompressionAlgorithm bedrockAlgorithm) {
            this.bedrockAlgorithm = bedrockAlgorithm;
            return this;
        }

        public CompressionAlgorithm register() {
            Preconditions.checkNotNull(this.identifier, "identifier");
            Preconditions.checkArgument(!types.containsKey(this.identifier), "CompressionAlgorithm " + this.identifier + " already exists");

            CompressionAlgorithm type = new CompressionAlgorithm(this.identifier, this.bedrockAlgorithm);
            types.put(this.identifier, type);
            return type;
        }
    }
}
