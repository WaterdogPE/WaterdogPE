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

import it.unimi.dsi.fastutil.bytes.Byte2ObjectMap;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectOpenHashMap;
import lombok.Getter;
import lombok.ToString;
import org.cloudburstmc.protocol.bedrock.data.CompressionAlgorithm;
import org.cloudburstmc.protocol.bedrock.data.PacketCompressionAlgorithm;
import org.cloudburstmc.protocol.common.util.Preconditions;

import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

@Getter
@ToString
public class CompressionType implements Comparable<CompressionType>, CompressionAlgorithm {
    private static final Map<String, CompressionType> types = new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER);
    private static final Byte2ObjectMap<CompressionType> headerIds = new Byte2ObjectOpenHashMap<>();

    public static final CompressionType NONE = CompressionType.builder()
            .identifier("none")
            .bedrockAlgorithm(PacketCompressionAlgorithm.NONE)
            .register();

    public static final CompressionType ZLIB = CompressionType.builder()
            .identifier("zlib")
            .bedrockAlgorithm(PacketCompressionAlgorithm.ZLIB)
            .register();

    public static final CompressionType SNAPPY = CompressionType.builder()
            .identifier("snappy")
            .bedrockAlgorithm(PacketCompressionAlgorithm.SNAPPY)
            .register();

    private final String identifier;
    private final PacketCompressionAlgorithm bedrockAlgorithm;
    private final byte headerId;

    private CompressionType(String identifier, PacketCompressionAlgorithm bedrockAlgorithm, byte headerId) {
        this.identifier = identifier;
        this.bedrockAlgorithm = bedrockAlgorithm;
        this.headerId = headerId;
    }

    public static CompressionType fromString(String string) {
        Preconditions.checkNotNull(string, "CompressionAlgorithm name can not be null");
        Preconditions.checkArgument(!string.isEmpty(), "CompressionAlgorithm name can not be empty");
        return types.get(string);
    }

    public static CompressionType fromHeaderId(byte headerId) {
        return headerIds.get(headerId);
    }

    public static CompressionType fromBedrockCompression(PacketCompressionAlgorithm algorithm) {
        return switch (algorithm) {
            case NONE -> NONE;
            case ZLIB -> ZLIB;
            case SNAPPY -> SNAPPY;
            default -> throw new UnsupportedOperationException("Unsupported compression " + algorithm);
        };
    }

    @Override
    public int compareTo(CompressionType o) {
        return this.identifier.compareTo(o.identifier);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String identifier;
        private PacketCompressionAlgorithm bedrockAlgorithm;
        private byte headerId = -1;

        public Builder identifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder bedrockAlgorithm(PacketCompressionAlgorithm bedrockAlgorithm) {
            this.bedrockAlgorithm = bedrockAlgorithm;
            return this;
        }

        public Builder headerId(byte headerId) {
            this.headerId = headerId;
            return this;
        }

        public CompressionType register() {
            Preconditions.checkNotNull(this.identifier, "identifier");
            Preconditions.checkArgument(!types.containsKey(this.identifier), "CompressionAlgorithm " + this.identifier + " already exists");

            if (this.bedrockAlgorithm == null && this.headerId < 0) {
                throw new IllegalArgumentException("Compression header must be set");
            }

            CompressionType type = new CompressionType(this.identifier, this.bedrockAlgorithm, this.headerId);
            types.put(this.identifier, type);
            headerIds.put(this.headerId, type);
            return type;
        }
    }
}
