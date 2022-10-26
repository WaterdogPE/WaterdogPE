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

package dev.waterdog.waterdogpe.network.session;

import com.google.common.base.Preconditions;
import com.nukkitx.protocol.bedrock.data.PacketCompressionAlgorithm;
import lombok.ToString;

import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

@ToString(exclude = "bedrockCompression")
public class CompressionAlgorithm implements Comparable<CompressionAlgorithm> {
    private static final Map<String, CompressionAlgorithm> types = new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER);
    private final String name;
    private final PacketCompressionAlgorithm bedrockCompression;

    public static final CompressionAlgorithm NONE = CompressionAlgorithm.fromString("none");
    public static final CompressionAlgorithm ZLIB = CompressionAlgorithm.fromString("zlib", PacketCompressionAlgorithm.ZLIB);
    public static final CompressionAlgorithm SNAPPY = CompressionAlgorithm.fromString("snappy", PacketCompressionAlgorithm.SNAPPY);

    private CompressionAlgorithm(String name, PacketCompressionAlgorithm bedrockCompression) {
        this.name = name;
        this.bedrockCompression = bedrockCompression;
    }

    public static CompressionAlgorithm fromString(String string) {
        return fromString(string, null);
    }

    private static CompressionAlgorithm fromString(String string, PacketCompressionAlgorithm bedrockCompression) {
        Preconditions.checkNotNull(string, "CompressionAlgorithm name can not be null");
        Preconditions.checkArgument(!string.isEmpty(), "CompressionAlgorithm name can not be empty");

        CompressionAlgorithm algorIthm = types.get(string);
        if (algorIthm == null) {
            types.put(string, algorIthm = new CompressionAlgorithm(string, bedrockCompression));
        }
        return algorIthm;
    }

    public String getName() {
        return this.name;
    }

    public PacketCompressionAlgorithm getBedrockCompression() {
        return this.bedrockCompression;
    }

    public static CompressionAlgorithm fromBedrockCompression(PacketCompressionAlgorithm algorithm) {
        return switch (algorithm) {
            case ZLIB -> ZLIB;
            case SNAPPY -> SNAPPY;
            default -> throw new UnsupportedOperationException("Unsupported compression " + algorithm);
        };
    }

    @Override
    public int compareTo(CompressionAlgorithm o) {
        return this.name.compareTo(o.name);
    }
}
