package pe.waterdog.network.protocol;

import com.google.common.base.Preconditions;
import com.nukkitx.protocol.bedrock.BedrockPacketCodec;
import pe.waterdog.ProxyServer;
import pe.waterdog.VersionInfo;
import pe.waterdog.logger.MainLogger;
import pe.waterdog.network.protocol.codec.BedrockCodec;
import pe.waterdog.network.protocol.codec.BedrockCodec408;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class ProtocolConstants {

    public static final int DEFAULT_RAKNET_VER = 10;

    public static final Map<Integer, ProtocolVersion> protocolMap = new HashMap<>();
    public static final Map<ProtocolVersion, BedrockCodec> protocol2CodecMap = new EnumMap<>(ProtocolVersion.class);

    static {
        for (ProtocolVersion protocol : ProtocolVersion.values()){
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
        return protocolMap.get(VersionInfo.LATEST_PROTOCOL_VERSION);
    }

    public static BedrockCodec getBedrockCodec(ProtocolVersion protocol){
        return protocol2CodecMap.get(protocol);
    }

    public static void registerCodecs(ProxyServer proxy){
        BedrockCodec codec_408 = new BedrockCodec408(ProtocolVersion.MINECRAFT_PE_1_16_20);
        registerCodec(codec_408);
    }

    public static void registerCodec(BedrockCodec bedrockCodec){
        Preconditions.checkNotNull(bedrockCodec.getProtocol(), "Protocol version can not be null!");
        Preconditions.checkArgument(!protocol2CodecMap.containsKey(bedrockCodec.getProtocol()), "BedrockCodec "+bedrockCodec.getProtocol()+" is registered!");

        //TODO: bedrock codec register event

        ProtocolVersion protocol = bedrockCodec.getProtocol();
        BedrockPacketCodec.Builder builder = bedrockCodec.createBuilder(protocol.getDefaultCodec());
        bedrockCodec.buildCodec(builder);

        protocol.setBedrockCodec(bedrockCodec);
        protocol2CodecMap.put(bedrockCodec.getProtocol(), bedrockCodec);

        MainLogger.getLogger().debug("Registered custom BedrockCodec "+protocol);
    }
}
