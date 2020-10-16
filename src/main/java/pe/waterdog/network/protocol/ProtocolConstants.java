package pe.waterdog.network.protocol;

import com.google.common.base.Preconditions;
import com.nukkitx.protocol.bedrock.BedrockPacketCodec;
import pe.waterdog.ProxyServer;
import pe.waterdog.VersionInfo;
import pe.waterdog.event.defaults.ProtocolCodecRegisterEvent;
import pe.waterdog.logger.MainLogger;
import pe.waterdog.network.protocol.codec.*;

import java.util.*;

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

    public static void registerCodecs(){
        registerCodec(ProtocolVersion.MINECRAFT_PE_1_13, new BedrockCodec388());
        registerCodec(ProtocolVersion.MINECRAFT_PE_1_14_30, new BedrockCodec389());
        registerCodec(ProtocolVersion.MINECRAFT_PE_1_14_60, new BedrockCodec390());
        registerCodec(ProtocolVersion.MINECRAFT_PE_1_16_20, new BedrockCodec408());
    }

    protected static boolean registerCodec(ProtocolVersion protocol, BedrockCodec bedrockCodec){
        Preconditions.checkArgument(!protocol2CodecMap.containsKey(protocol), "BedrockCodec "+protocol+" is registered!");
        Preconditions.checkArgument(protocol == bedrockCodec.getProtocol(), "Protocol versions does not match!");

        BedrockPacketCodec.Builder builder = bedrockCodec.createBuilder(protocol.getDefaultCodec());
        bedrockCodec.buildCodec(builder);

        ProtocolCodecRegisterEvent event = new ProtocolCodecRegisterEvent(protocol, builder);
        ProxyServer.getInstance().getEventManager().callEvent(event);
        if (event.isCancelled()){
            return false;
        }

        bedrockCodec.setPacketCodec(builder.build());
        protocol.setBedrockCodec(bedrockCodec);
        protocol2CodecMap.put(protocol, bedrockCodec);

        MainLogger.getLogger().debug("Registered custom BedrockCodec "+protocol);
        return true;
    }
}
