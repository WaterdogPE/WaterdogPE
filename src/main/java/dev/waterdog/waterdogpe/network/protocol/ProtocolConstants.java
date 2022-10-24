/*
 * Copyright 2021 WaterdogTEAM
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

import com.google.common.base.Preconditions;
import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.WaterdogPE;
import dev.waterdog.waterdogpe.network.protocol.codec.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;

import java.util.EnumMap;
import java.util.Map;

/**
 * Constants for all currently supported versions of the Minecraft: Bedrock Edition
 */
public class ProtocolConstants {

    public static final Object2ObjectArrayMap<Integer, ProtocolVersion> protocolMap = new Object2ObjectArrayMap<>();
    public static final Map<ProtocolVersion, BedrockCodec> protocol2CodecMap = new EnumMap<>(ProtocolVersion.class);

    static {
        for (ProtocolVersion protocol : ProtocolVersion.values()) {
            protocolMap.put(protocol.getProtocol(), protocol);
        }
    }

    public static boolean isAccepted(int protocol) {
        return protocolMap.containsKey(protocol);
    }

    public static ProtocolVersion get(int protocol) {
        return protocolMap.get(protocol);
    }

    public static ProtocolVersion getLatestProtocol() {
        return protocolMap.get(WaterdogPE.version().latestProtocolVersion());
    }

    public static BedrockCodec getBedrockCodec(ProtocolVersion protocol) {
        return protocol2CodecMap.get(protocol);
    }

    /**
     * Here we register customized, performance improved codecs for supported game versions.
     */
    public static void registerCodecs() {
        registerCodec(ProtocolVersion.MINECRAFT_PE_1_8, new BedrockCodec313());
        registerCodec(ProtocolVersion.MINECRAFT_PE_1_9, new BedrockCodec332());
        registerCodec(ProtocolVersion.MINECRAFT_PE_1_10, new BedrockCodec340());
        registerCodec(ProtocolVersion.MINECRAFT_PE_1_11, new BedrockCodec354());
        registerCodec(ProtocolVersion.MINECRAFT_PE_1_12, new BedrockCodec361());
        registerCodec(ProtocolVersion.MINECRAFT_PE_1_13, new BedrockCodec388());
        registerCodec(ProtocolVersion.MINECRAFT_PE_1_14_30, new BedrockCodec389());
        registerCodec(ProtocolVersion.MINECRAFT_PE_1_14_60, new BedrockCodec390());
        registerCodec(ProtocolVersion.MINECRAFT_PE_1_16, new BedrockCodec407());
        registerCodec(ProtocolVersion.MINECRAFT_PE_1_16_20, new BedrockCodec408());
        registerCodec(ProtocolVersion.MINECRAFT_PE_1_16_100, new BedrockCodec419());
        registerCodec(ProtocolVersion.MINECRAFT_PE_1_16_200, new BedrockCodec422());
        registerCodec(ProtocolVersion.MINECRAFT_PE_1_16_210, new BedrockCodec428());
        registerCodec(ProtocolVersion.MINECRAFT_PE_1_16_220, new BedrockCodec431());
        registerCodec(ProtocolVersion.MINECRAFT_PE_1_17_0, new BedrockCodec440());
        registerCodec(ProtocolVersion.MINECRAFT_PE_1_17_10, new BedrockCodec448());
        registerCodec(ProtocolVersion.MINECRAFT_PE_1_17_30, new BedrockCodec465());
        registerCodec(ProtocolVersion.MINECRAFT_PE_1_17_40, new BedrockCodec471());
        registerCodec(ProtocolVersion.MINECRAFT_PE_1_18_0, new BedrockCodec475());
        registerCodec(ProtocolVersion.MINECRAFT_PE_1_18_10, new BedrockCodec486());
        registerCodec(ProtocolVersion.MINECRAFT_PE_1_18_30, new BedrockCodec503());
        registerCodec(ProtocolVersion.MINECRAFT_PE_1_19_0, new BedrockCodec527());
        registerCodec(ProtocolVersion.MINECRAFT_PE_1_19_10, new BedrockCodec534());
        registerCodec(ProtocolVersion.MINECRAFT_PE_1_19_20, new BedrockCodec544());
        registerCodec(ProtocolVersion.MINECRAFT_PE_1_19_21, new BedrockCodec545());
        registerCodec(ProtocolVersion.MINECRAFT_PE_1_19_30, new BedrockCodec554());
    }

    /**
     * Register BedrockCodec for specific protocol version.
     *
     * @param protocol     protocol version matched for instance of BedrockCodec.
     * @param bedrockCodec must match same protocol version as protocol or exception will be thrown.
     * @return if registration was not canceled by plugin.
     */
    protected static boolean registerCodec(ProtocolVersion protocol, BedrockCodec bedrockCodec) {
        Preconditions.checkArgument(!protocol2CodecMap.containsKey(protocol), "BedrockCodec " + protocol + " is registered!");
        Preconditions.checkArgument(protocol == bedrockCodec.getProtocol(), "Protocol versions does not match!");

        ProxyServer proxy = ProxyServer.getInstance();
        boolean success = bedrockCodec.initializeCodec(protocol, proxy);
        if (!success) {
            return false;
        }

        protocol.setBedrockCodec(bedrockCodec);
        protocol2CodecMap.put(protocol, bedrockCodec);
        proxy.getLogger().debug("Registered custom BedrockCodec " + protocol);
        return true;
    }
}
