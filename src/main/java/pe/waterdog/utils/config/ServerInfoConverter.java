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
    public Object toConfig(Class<?> type, Object obj, ParameterizedType parameterizedType) throws Exception {
        Map<String, String> map = new HashMap<>();
        ServerInfo i = (ServerInfo) obj;
        map.put("address", i.getAddress().getAddress().getHostAddress() + ":" + i.getAddress().getPort());
        if (i.getPublicAddress() != null && i.getPublicAddress() != i.getAddress()) {
            map.put("public_address", i.getAddress().getAddress().getHostAddress() + ":" + i.getAddress().getPort());
        }
        return map;
    }

    @Override
    public Object fromConfig(Class<?> type, Object obj, ParameterizedType parameterizedType) throws Exception {
        if (obj == null) return null;
        Converter inetConverter = internalConverter.getConverter(InetSocketAddress.class);
        String name;
        InetSocketAddress address;
        InetSocketAddress publicAddress;
        if (obj instanceof ConfigSection) {
            ConfigSection section = (ConfigSection) obj;
            name = section.get("name");
            address = (InetSocketAddress) inetConverter.fromConfig(InetSocketAddress.class, section.get("address"), null);
            publicAddress = (InetSocketAddress) inetConverter.fromConfig(InetSocketAddress.class, section.get("public_address"), null);

        } else if (obj instanceof Map) {
            name = (String) ((Map) obj).get("name");
            address = (InetSocketAddress) inetConverter.fromConfig(InetSocketAddress.class, ((Map) obj).get("address"), null);
            publicAddress = (InetSocketAddress) inetConverter.fromConfig(InetSocketAddress.class, ((Map) obj).get("public_address"), null);
        } else {
            throw new IllegalArgumentException("ServerInfoConverter#fromConfig cannot parse obj: " + obj.getClass().getName());
        }
        return new ServerInfo(name, address, publicAddress);

    }

    @Override
    public boolean supports(Class<?> type) {
        return ServerInfo.class.isAssignableFrom(type);
    }
}
