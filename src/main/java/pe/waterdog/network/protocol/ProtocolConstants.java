package pe.waterdog.network.protocol;

import com.nukkitx.protocol.bedrock.BedrockPacketCodec;
import com.nukkitx.protocol.bedrock.v388.Bedrock_v388;
import com.nukkitx.protocol.bedrock.v389.Bedrock_v389;
import com.nukkitx.protocol.bedrock.v390.Bedrock_v390;
import com.nukkitx.protocol.bedrock.v407.Bedrock_v407;
import com.nukkitx.protocol.bedrock.v408.Bedrock_v408;
import pe.waterdog.VersionInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * Constants for all currently supported versions of the Minecraft: Bedrock Edition
 */
public class ProtocolConstants {

    public static final int DEFAULT_RAKNET_VER = 10;
    public static final Map<Integer, Protocol> protocolMap = new HashMap<>();

    static {
        for (Protocol protocol : Protocol.values()) protocolMap.put(protocol.getProtocol(), protocol);
    }

    public static boolean isAccepted(int protocol) {
        return protocolMap.containsKey(protocol);
    }

    public static Protocol get(int protocol) {
        return protocolMap.get(protocol);
    }

    public static Protocol getLatestProtocol() {
        return protocolMap.get(VersionInfo.LATEST_PROTOCOL_VERSION);
    }

    public enum Protocol {
        MINECRAFT_PE_1_13(388, Bedrock_v388.V388_CODEC, 9),
        MINECRAFT_PE_1_14_30(389, Bedrock_v389.V389_CODEC, 9),
        MINECRAFT_PE_1_14_60(390, Bedrock_v390.V390_CODEC, 9),
        MINECRAFT_PE_1_16(407, Bedrock_v407.V407_CODEC),
        MINECRAFT_PE_1_16_20(408, Bedrock_v408.V408_CODEC);

        private final int protocol;
        private final BedrockPacketCodec codec;
        private final int raknetVersion;

        Protocol(int protocol, BedrockPacketCodec codec) {
            this(protocol, codec, DEFAULT_RAKNET_VER);
        }

        Protocol(int protocol, BedrockPacketCodec codec, int raknetVersion) {
            this.protocol = protocol;
            this.codec = codec;
            this.raknetVersion = raknetVersion;
        }

        public int getProtocol() {
            return this.protocol;
        }

        public BedrockPacketCodec getCodec() {
            return this.codec;
        }

        public int getRaknetVersion() {
            return this.raknetVersion;
        }
    }
}
