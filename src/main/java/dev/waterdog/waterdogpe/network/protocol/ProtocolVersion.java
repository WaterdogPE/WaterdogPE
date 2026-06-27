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

package dev.waterdog.waterdogpe.network.protocol;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.codec.v1001.Bedrock_v1001;
import org.cloudburstmc.protocol.bedrock.codec.v313.Bedrock_v313;
import org.cloudburstmc.protocol.bedrock.codec.v332.Bedrock_v332;
import org.cloudburstmc.protocol.bedrock.codec.v340.Bedrock_v340;
import org.cloudburstmc.protocol.bedrock.codec.v354.Bedrock_v354;
import org.cloudburstmc.protocol.bedrock.codec.v361.Bedrock_v361;
import org.cloudburstmc.protocol.bedrock.codec.v388.Bedrock_v388;
import org.cloudburstmc.protocol.bedrock.codec.v389.Bedrock_v389;
import org.cloudburstmc.protocol.bedrock.codec.v390.Bedrock_v390;
import org.cloudburstmc.protocol.bedrock.codec.v407.Bedrock_v407;
import org.cloudburstmc.protocol.bedrock.codec.v408.Bedrock_v408;
import org.cloudburstmc.protocol.bedrock.codec.v419.Bedrock_v419;
import org.cloudburstmc.protocol.bedrock.codec.v422.Bedrock_v422;
import org.cloudburstmc.protocol.bedrock.codec.v428.Bedrock_v428;
import org.cloudburstmc.protocol.bedrock.codec.v431.Bedrock_v431;
import org.cloudburstmc.protocol.bedrock.codec.v440.Bedrock_v440;
import org.cloudburstmc.protocol.bedrock.codec.v448.Bedrock_v448;
import org.cloudburstmc.protocol.bedrock.codec.v465.Bedrock_v465;
import org.cloudburstmc.protocol.bedrock.codec.v471.Bedrock_v471;
import org.cloudburstmc.protocol.bedrock.codec.v475.Bedrock_v475;
import org.cloudburstmc.protocol.bedrock.codec.v486.Bedrock_v486;
import org.cloudburstmc.protocol.bedrock.codec.v503.Bedrock_v503;
import org.cloudburstmc.protocol.bedrock.codec.v527.Bedrock_v527;
import org.cloudburstmc.protocol.bedrock.codec.v534.Bedrock_v534;
import org.cloudburstmc.protocol.bedrock.codec.v544.Bedrock_v544;
import org.cloudburstmc.protocol.bedrock.codec.v545.Bedrock_v545;
import org.cloudburstmc.protocol.bedrock.codec.v554.Bedrock_v554;
import org.cloudburstmc.protocol.bedrock.codec.v557.Bedrock_v557;
import org.cloudburstmc.protocol.bedrock.codec.v560.Bedrock_v560;
import org.cloudburstmc.protocol.bedrock.codec.v567.Bedrock_v567;
import org.cloudburstmc.protocol.bedrock.codec.v568.Bedrock_v568;
import org.cloudburstmc.protocol.bedrock.codec.v575.Bedrock_v575;
import org.cloudburstmc.protocol.bedrock.codec.v582.Bedrock_v582;
import org.cloudburstmc.protocol.bedrock.codec.v589.Bedrock_v589;
import org.cloudburstmc.protocol.bedrock.codec.v594.Bedrock_v594;
import org.cloudburstmc.protocol.bedrock.codec.v618.Bedrock_v618;
import org.cloudburstmc.protocol.bedrock.codec.v622.Bedrock_v622;
import org.cloudburstmc.protocol.bedrock.codec.v630.Bedrock_v630;
import org.cloudburstmc.protocol.bedrock.codec.v649.Bedrock_v649;
import org.cloudburstmc.protocol.bedrock.codec.v662.Bedrock_v662;
import org.cloudburstmc.protocol.bedrock.codec.v671.Bedrock_v671;
import org.cloudburstmc.protocol.bedrock.codec.v685.Bedrock_v685;
import org.cloudburstmc.protocol.bedrock.codec.v686.Bedrock_v686;
import org.cloudburstmc.protocol.bedrock.codec.v712.Bedrock_v712;
import org.cloudburstmc.protocol.bedrock.codec.v729.Bedrock_v729;
import org.cloudburstmc.protocol.bedrock.codec.v748.Bedrock_v748;
import org.cloudburstmc.protocol.bedrock.codec.v766.Bedrock_v766;
import org.cloudburstmc.protocol.bedrock.codec.v776.Bedrock_v776;
import org.cloudburstmc.protocol.bedrock.codec.v786.Bedrock_v786;
import org.cloudburstmc.protocol.bedrock.codec.v800.Bedrock_v800;
import org.cloudburstmc.protocol.bedrock.codec.v818.Bedrock_v818;
import org.cloudburstmc.protocol.bedrock.codec.v819.Bedrock_v819;
import org.cloudburstmc.protocol.bedrock.codec.v827.Bedrock_v827;
import org.cloudburstmc.protocol.bedrock.codec.v844.Bedrock_v844;
import org.cloudburstmc.protocol.bedrock.codec.v859.Bedrock_v859;
import org.cloudburstmc.protocol.bedrock.codec.v860.Bedrock_v860;
import org.cloudburstmc.protocol.bedrock.codec.v898.Bedrock_v898;
import org.cloudburstmc.protocol.bedrock.codec.v924.Bedrock_v924;
import org.cloudburstmc.protocol.bedrock.codec.v944.Bedrock_v944;
import org.cloudburstmc.protocol.bedrock.codec.v975.Bedrock_v975;

@ToString(exclude = {"defaultCodec", "bedrockCodec"})
public enum ProtocolVersion {
    MINECRAFT_PE_1_8(313, Bedrock_v313.CODEC, "1.8.0", "1.8.1"),
    MINECRAFT_PE_1_9(332, Bedrock_v332.CODEC, "1.9.0"),
    MINECRAFT_PE_1_10(340, Bedrock_v340.CODEC, "1.10.0", "1.10.1"),
    MINECRAFT_PE_1_11(354, Bedrock_v354.CODEC, "1.11.0", "1.11.1", "1.11.2", "1.11.3", "1.11.4"),
    MINECRAFT_PE_1_12(361, Bedrock_v361.CODEC, "1.12.0", "1.12.1"),
    MINECRAFT_PE_1_13(388, Bedrock_v388.CODEC, "1.13.0", "1.13.1", "1.13.2", "1.13.3", "1.14.0"),
    MINECRAFT_PE_1_14(389, Bedrock_v389.CODEC, "1.14.0", "1.14.1", "1.14.20", "1.14.30", "1.14.41", "1.14.60"),
    MINECRAFT_PE_1_14_60(390, Bedrock_v390.CODEC, "1.14.60"),
    MINECRAFT_PE_1_16(407, Bedrock_v407.CODEC, "1.16.0", "1.16.1", "1.16.1.03", "1.16.1.04", "1.16.10"),
    MINECRAFT_PE_1_16_20(408, Bedrock_v408.CODEC, "1.16.20", "1.16.21", "1.16.40", "1.16.42", "1.16.50", "1.16.60", "1.16.61"),
    MINECRAFT_PE_1_16_100(419, Bedrock_v419.CODEC, "1.16.100", "1.16.1001"),
    MINECRAFT_PE_1_16_200(422, Bedrock_v422.CODEC, "1.16.200", "1.16.201"),
    MINECRAFT_PE_1_16_210(428, Bedrock_v428.CODEC, "1.16.210"),
    MINECRAFT_PE_1_16_220(431, Bedrock_v431.CODEC, "1.16.220", "1.16.221"),
    MINECRAFT_PE_1_17_0(440, Bedrock_v440.CODEC, "1.17.0", "1.17.1", "1.17.2"),
    MINECRAFT_PE_1_17_10(448, Bedrock_v448.CODEC, "1.17.10", "1.17.11"),
    MINECRAFT_PE_1_17_30(465, Bedrock_v465.CODEC, "1.17.30", "1.17.32", "1.17.33", "1.17.34"),
    MINECRAFT_PE_1_17_40(471, Bedrock_v471.CODEC, "1.17.40", "1.17.41"),
    MINECRAFT_PE_1_18_0(475, Bedrock_v475.CODEC, "1.18.0", "1.18.1", "1.18.2"),
    MINECRAFT_PE_1_18_10(486, Bedrock_v486.CODEC, "1.18.10", "1.18.11"),
    MINECRAFT_PE_1_18_30(503, Bedrock_v503.CODEC, "1.18.30", "1.18.31", "1.18.32", "1.18.33"),
    MINECRAFT_PE_1_19_0(527, Bedrock_v527.CODEC, "1.19.0", "1.19.2"),
    MINECRAFT_PE_1_19_10(534, Bedrock_v534.CODEC, "1.19.10", "1.19.11"),
    MINECRAFT_PE_1_19_20(544, Bedrock_v544.CODEC, "1.19.20"),
    MINECRAFT_PE_1_19_21(545, Bedrock_v545.CODEC, "1.19.21", "1.19.22"),
    MINECRAFT_PE_1_19_30(554, Bedrock_v554.CODEC, "1.19.30", "1.19.31"),
    MINECRAFT_PE_1_19_40(557, Bedrock_v557.CODEC, "1.19.40", "1.19.41"),
    MINECRAFT_PE_1_19_50(560, Bedrock_v560.CODEC, "1.19.50", "1.19.51"),
    MINECRAFT_PE_1_19_60(567, Bedrock_v567.CODEC, "1.19.60"),
    MINECRAFT_PE_1_19_62(567, 568, Bedrock_v568.CODEC, "1.19.62"), // this version has not bumped protocol number on client side
    MINECRAFT_PE_1_19_63(568, Bedrock_v568.CODEC, "1.19.63"),
    MINECRAFT_PE_1_19_70(575, Bedrock_v575.CODEC, "1.19.70", "1.19.71"),
    MINECRAFT_PE_1_19_80(582, Bedrock_v582.CODEC, "1.19.80", "1.19.81", "1.19.83"),
    MINECRAFT_PE_1_20_0(589, Bedrock_v589.CODEC, "1.20.0", "1.20.1"),
    MINECRAFT_PE_1_20_10(594, Bedrock_v594.CODEC, "1.20.10", "1.20.12", "1.20.13", "1.20.14", "1.20.15"),
    MINECRAFT_PE_1_20_30(618, Bedrock_v618.CODEC, "1.20.30", "1.20.31", "1.20.32"),
    MINECRAFT_PE_1_20_40(622, Bedrock_v622.CODEC, "1.20.40", "1.20.41"),
    MINECRAFT_PE_1_20_50(630, Bedrock_v630.CODEC, "1.20.50", "1.20.51"),
    MINECRAFT_PE_1_20_60(649, Bedrock_v649.CODEC,"1.20.60", "1.20.62"),
    MINECRAFT_PE_1_20_70(662, Bedrock_v662.CODEC, "1.20.70", "1.20.71", "1.20.72", "1.20.73"),
    MINECRAFT_PE_1_20_80(671, Bedrock_v671.CODEC, "1.20.80", "1.20.81"),
    MINECRAFT_PE_1_21_0(685, Bedrock_v685.CODEC, "1.21.0", "1.21.1"),
    MINECRAFT_PE_1_21_2(686, Bedrock_v686.CODEC, "1.21.2", "1.21.3"),
    MINECRAFT_PE_1_21_20(712, Bedrock_v712.CODEC, "1.21.20", "1.21.21", "1.21.22", "1.21.23"),
    MINECRAFT_PE_1_21_30(729, Bedrock_v729.CODEC, "1.21.30", "1.21.31"),
    MINECRAFT_PE_1_21_40(748, Bedrock_v748.CODEC, "1.21.40", "1.21.41", "1.21.42", "1.21.43", "1.21.44"),
    MINECRAFT_PE_1_21_50(766, Bedrock_v766.CODEC, "1.21.50", "1.21.51"),
    MINECRAFT_PE_1_21_60(776, Bedrock_v776.CODEC, "1.21.60", "1.21.61", "1.21.62"),
    MINECRAFT_PE_1_21_70(786, Bedrock_v786.CODEC, "1.21.70", "1.21.71", "1.21.72", "1.21.73"),
    MINECRAFT_PE_1_21_80(800, Bedrock_v800.CODEC, "1.21.80", "1.21.81", "1.21.82", "1.21.84"),
    MINECRAFT_PE_1_21_90(818, Bedrock_v818.CODEC, "1.21.90", "1.21.91", "1.21.92"),
    MINECRAFT_PE_1_21_93(819, Bedrock_v819.CODEC, "1.21.93", "1.21.94"),
    MINECRAFT_PE_1_21_100(827, Bedrock_v827.CODEC, "1.21.100", "1.21.101"),
    MINECRAFT_PE_1_21_110(843, 844, Bedrock_v844.CODEC, "1.21.110"),
    MINECRAFT_PE_1_21_111(844, Bedrock_v844.CODEC, "1.21.111", "1.21.112", "1.21.113", "1.21.114"),
    MINECRAFT_PE_1_21_120(859, Bedrock_v859.CODEC, "1.21.120", "1.21.121", "1.21.123"),
    MINECRAFT_PE_1_21_124(860, Bedrock_v860.CODEC, "1.21.124"),
    MINECRAFT_PE_1_21_130(898, Bedrock_v898.CODEC, "1.21.130", "1.21.131", "1.21.132"),
    MINECRAFT_PE_1_26_0(924, Bedrock_v924.CODEC, "26.0", "26.1", "26.2", "26.3"),
    MINECRAFT_PE_1_26_10(944, Bedrock_v944.CODEC, "26.10", "26.11", "26.11", "26.12", "26.13"),
    MINECRAFT_PE_1_26_20(975, Bedrock_v975.CODEC, "26.20", "26.21", "26.22", "26.23"),
    MINECRAFT_PE_1_26_30(1001, Bedrock_v1001.CODEC, "26.30", "26.31", "26.32"),
    ;

    private static final ProtocolVersion[] VALUES = values();
    private static final Int2ObjectMap<ProtocolVersion> VERSIONS = new Int2ObjectOpenHashMap<>();
    static {
        for (ProtocolVersion version : values()) {
            VERSIONS.putIfAbsent(version.getProtocol(), version);
        }
    }

    @Getter
    private final int protocol;
    @Getter
    private final int protocolInternal;

    @Getter
    private final BedrockCodec defaultCodec;
    @Setter
    private BedrockCodec bedrockCodec;
    @Getter
    private final ObjectImmutableList<String> displayNames;

    ProtocolVersion(int protocol, BedrockCodec codec, String... displayNames) {
        this(protocol, protocol, codec, displayNames);
    }

    ProtocolVersion(int protocol, int protocolInternal, BedrockCodec codec, String... displayNames) {
        this.protocol = protocol;
        this.protocolInternal = protocolInternal;
        this.defaultCodec = codec;
        this.displayNames = new ObjectImmutableList<>(displayNames);
    }

    public boolean isBefore(ProtocolVersion version) {
        return this.protocolInternal < version.protocolInternal;
    }

    public boolean isBeforeOrEqual(ProtocolVersion version) {
        return this.protocolInternal <= version.protocolInternal;
    }

    public boolean isAfter(ProtocolVersion version) {
        return this.protocolInternal > version.protocolInternal;
    }

    public boolean isAfterOrEqual(ProtocolVersion version) {
        return this.protocolInternal >= version.protocolInternal;
    }

    public int getRaknetVersion() {
        return this.getCodec().getRaknetProtocolVersion();
    }

    public BedrockCodec getCodec() {
        return this.bedrockCodec == null ? this.defaultCodec : this.bedrockCodec;
    }

    public String getMinecraftVersion() {
        return this.getCodec().getMinecraftVersion();
    }

    public static ProtocolVersion latest() {
        return VALUES[VALUES.length - 1];
    }

    public static ProtocolVersion oldest() {
        return VALUES[0];
    }

    public static ProtocolVersion get(int protocol) {
        return VERSIONS.get(protocol);
    }
}
