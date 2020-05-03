package pe.waterdog.network.protocol;

import com.nukkitx.protocol.bedrock.BedrockPacketCodec;
import com.nukkitx.protocol.bedrock.v388.Bedrock_v388;
import com.nukkitx.protocol.bedrock.v389.Bedrock_v389;
import com.nukkitx.protocol.bedrock.v390.Bedrock_v390;
import pe.waterdog.VersionInfo;

import java.util.*;

public class ProtocolConstants {

    public enum Protocol{
        MINECRAFT_PE_1_13(388, Bedrock_v388.V388_CODEC),
        MINECRAFT_PE_1_14_30(389, Bedrock_v389.V389_CODEC),
        MINECRAFT_PE_1_14_60(390, Bedrock_v390.V390_CODEC);


        private final int protocol;
        private final BedrockPacketCodec codec;

        Protocol(int protocol, BedrockPacketCodec codec) {
            this.protocol = protocol;
            this.codec = codec;
        }

        public int getProtocol() {
            return protocol;
        }

        public BedrockPacketCodec getCodec() {
            return codec;
        }
    }
    public static Map<Integer, Protocol> protocolMap = new HashMap<>();
    static {
        for (Protocol protocol : Protocol.values()) protocolMap.put(protocol.getProtocol(), protocol);
    }

    public static boolean isAccepted(int protocol){
        return protocolMap.containsKey(protocol);
    }

    public static Protocol get(int protocol){
        return protocolMap.get(protocol);
    }

    public static Protocol getLatestProtocol(){
        return protocolMap.get(VersionInfo.LATEST_PROTOCOL_VERSION);
    }
}
