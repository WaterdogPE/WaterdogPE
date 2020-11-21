package pe.waterdog.utils.config;

import net.cubespace.Yamler.Config.ConfigSection;
import net.cubespace.Yamler.Config.Converter.Converter;
import net.cubespace.Yamler.Config.InternalConverter;
import pe.waterdog.network.ServerInfo;

import java.lang.reflect.ParameterizedType;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public class ServerInfoConverter implements Converter {

    private final InternalConverter internalConverter;

    public ServerInfoConverter(InternalConverter internalConverter) {
        this.internalConverter = internalConverter;
    }

    @Override
    public Object toConfig(Class<?> type, Object object, ParameterizedType parameterizedType) throws Exception {
        Map<String, String> map = new HashMap<>();
        ServerInfo serverInfo = (ServerInfo) object;
        map.put("address", serverInfo.getAddress().getAddress().getHostAddress() + ":" + serverInfo.getAddress().getPort());
        if (serverInfo.getPublicAddress() != null && serverInfo.getPublicAddress() != serverInfo.getAddress()) {
            map.put("public_address", serverInfo.getAddress().getAddress().getHostAddress() + ":" + serverInfo.getAddress().getPort());
        }
        return map;
    }

    @Override
    public Object fromConfig(Class<?> type, Object object, ParameterizedType parameterizedType) throws Exception {
        if (object == null) {
            return null;
        }
        Converter inetConverter = internalConverter.getConverter(InetSocketAddress.class);
        InetSocketAddress address;
        InetSocketAddress publicAddress;

        if (object instanceof ConfigSection) {
            ConfigSection section = (ConfigSection) object;
            address = (InetSocketAddress) inetConverter.fromConfig(InetSocketAddress.class, section.get("address"), null);
            publicAddress = (InetSocketAddress) inetConverter.fromConfig(InetSocketAddress.class, section.get("public_address"), null);
            return new ServerInfo(section.get("name"), address, publicAddress);
        }

        if (object instanceof Map) {
            Map<?, Map> map = (Map<?, Map>) object;
            for (Map.Entry<?, Map> subMap : map.entrySet()) {
                address = (InetSocketAddress) inetConverter.fromConfig(InetSocketAddress.class, subMap.getValue().get("address"), null);
                publicAddress = (InetSocketAddress) inetConverter.fromConfig(InetSocketAddress.class, subMap.getValue().get("public_address"), null);
                return new ServerInfo((String) subMap.getKey(), address, publicAddress);
            }
        }
        throw new IllegalArgumentException("ServerInfoConverter#fromConfig cannot parse obj: " + object.getClass().getName());
    }

    @Override
    public boolean supports(Class<?> type) {
        return ServerInfo.class.isAssignableFrom(type);
    }
}
