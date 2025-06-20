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
import lombok.ToString;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
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

@ToString(exclude = {"defaultCodec", "bedrockCodec"})
public enum ProtocolVersion {

    MINECRAFT_PE_1_8(313, Bedrock_v313.CODEC),
    MINECRAFT_PE_1_9(332, Bedrock_v332.CODEC),
    MINECRAFT_PE_1_10(340, Bedrock_v340.CODEC),
    MINECRAFT_PE_1_11(354, Bedrock_v354.CODEC),
    MINECRAFT_PE_1_12(361, Bedrock_v361.CODEC),
    MINECRAFT_PE_1_13(388, Bedrock_v388.CODEC),
    MINECRAFT_PE_1_14_30(389, Bedrock_v389.CODEC),
    MINECRAFT_PE_1_14_60(390, Bedrock_v390.CODEC),
    MINECRAFT_PE_1_16(407, Bedrock_v407.CODEC),
    MINECRAFT_PE_1_16_20(408, Bedrock_v408.CODEC),
    MINECRAFT_PE_1_16_100(419, Bedrock_v419.CODEC),
    MINECRAFT_PE_1_16_200(422, Bedrock_v422.CODEC),
    MINECRAFT_PE_1_16_210(428, Bedrock_v428.CODEC),
    MINECRAFT_PE_1_16_220(431, Bedrock_v431.CODEC),
    MINECRAFT_PE_1_17_0(440, Bedrock_v440.CODEC),
    MINECRAFT_PE_1_17_10(448, Bedrock_v448.CODEC),
    MINECRAFT_PE_1_17_30(465, Bedrock_v465.CODEC),
    MINECRAFT_PE_1_17_40(471, Bedrock_v471.CODEC),
    MINECRAFT_PE_1_18_0(475, Bedrock_v475.CODEC),
    MINECRAFT_PE_1_18_10(486, Bedrock_v486.CODEC),
    MINECRAFT_PE_1_18_30(503, Bedrock_v503.CODEC),
    MINECRAFT_PE_1_19_0(527, Bedrock_v527.CODEC),
    MINECRAFT_PE_1_19_10(534, Bedrock_v534.CODEC),
    MINECRAFT_PE_1_19_20(544, Bedrock_v544.CODEC),
    MINECRAFT_PE_1_19_21(545, Bedrock_v545.CODEC),
    MINECRAFT_PE_1_19_30(554, Bedrock_v554.CODEC),
    MINECRAFT_PE_1_19_40(557, Bedrock_v557.CODEC),
    MINECRAFT_PE_1_19_50(560, Bedrock_v560.CODEC),
    MINECRAFT_PE_1_19_60(567, Bedrock_v567.CODEC),
    MINECRAFT_PE_1_19_62(567, 568, Bedrock_v568.CODEC), // this version has not bumped protocol number on client side
    MINECRAFT_PE_1_19_63(568, Bedrock_v568.CODEC),
    MINECRAFT_PE_1_19_70(575, Bedrock_v575.CODEC),
    MINECRAFT_PE_1_19_80(582, Bedrock_v582.CODEC),
    MINECRAFT_PE_1_20_0(589, Bedrock_v589.CODEC),
    MINECRAFT_PE_1_20_10(594, Bedrock_v594.CODEC),
    MINECRAFT_PE_1_20_30(618, Bedrock_v618.CODEC),
    MINECRAFT_PE_1_20_40(622, Bedrock_v622.CODEC),
    MINECRAFT_PE_1_20_50(630, Bedrock_v630.CODEC),
    MINECRAFT_PE_1_20_60(649, Bedrock_v649.CODEC),
    MINECRAFT_PE_1_20_70(662, Bedrock_v662.CODEC),
    MINECRAFT_PE_1_20_80(671, Bedrock_v671.CODEC),
    MINECRAFT_PE_1_21_0(685, Bedrock_v685.CODEC),
    MINECRAFT_PE_1_21_2(686, Bedrock_v686.CODEC),
    MINECRAFT_PE_1_21_20(712, Bedrock_v712.CODEC),
    MINECRAFT_PE_1_21_30(729, Bedrock_v729.CODEC),
    MINECRAFT_PE_1_21_40(748, Bedrock_v748.CODEC),
    MINECRAFT_PE_1_21_50_29(765, 766, Bedrock_v766.CODEC),
    MINECRAFT_PE_1_21_50(766, Bedrock_v766.CODEC),
    MINECRAFT_PE_1_21_60(776, Bedrock_v776.CODEC),
    MINECRAFT_PE_1_21_70(786, Bedrock_v786.CODEC),
    MINECRAFT_PE_1_21_80(800, Bedrock_v800.CODEC),
    MINECRAFT_PE_1_21_90(818, Bedrock_v818.CODEC),
    ;

    private static final ProtocolVersion[] VALUES = values();
    private static final Int2ObjectMap<ProtocolVersion> VERSIONS = new Int2ObjectOpenHashMap<>();
    static {
        for (ProtocolVersion version : values()) {
            VERSIONS.putIfAbsent(version.getProtocol(), version);
        }
    }

    private final int protocol;
    private final int protocolInternal;

    private final BedrockCodec defaultCodec;
    private BedrockCodec bedrockCodec;

    ProtocolVersion(int protocol, BedrockCodec codec) {
        this(protocol, protocol, codec);
    }

    ProtocolVersion(int protocol, int protocolInternal, BedrockCodec codec) {
        this.protocol = protocol;
        this.protocolInternal = protocolInternal;
        this.defaultCodec = codec;
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

    public int getProtocol() {
        return this.protocol;
    }

    public int getProtocolInternal() {
        return this.protocolInternal;
    }

    public int getRaknetVersion() {
        return this.getCodec().getRaknetProtocolVersion();
    }

    public BedrockCodec getDefaultCodec() {
        return this.defaultCodec;
    }

    public BedrockCodec getCodec() {
        return this.bedrockCodec == null ? this.defaultCodec : this.bedrockCodec;
    }

    public void setBedrockCodec(BedrockCodec bedrockCodec) {
        this.bedrockCodec = bedrockCodec;
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
