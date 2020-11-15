package pe.waterdog.utils.config;

import net.cubespace.Yamler.Config.ConfigSection;
import net.cubespace.Yamler.Config.Converter.Converter;
import net.cubespace.Yamler.Config.InternalConverter;
import pe.waterdog.network.ServerInfo;

import java.lang.reflect.ParameterizedType;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public class ServerListConverter implements Converter {

    private final InternalConverter internalConverter;

    public ServerListConverter(InternalConverter internalConverter) {
        this.internalConverter = internalConverter;
    }

    @Override
    public Object toConfig(Class<?> type, Object obj, ParameterizedType parameterizedType) throws RuntimeException {
        ServerList list = (ServerList) obj;
        ConfigSection section = new ConfigSection();
        Converter converter = internalConverter.getConverter(InetSocketAddress.class);
        list.values().forEach(serverInfo -> {
            Map<String, Object> map = new HashMap<>(2);
            try {
                map.put("address", converter.toConfig(InetSocketAddress.class, serverInfo.getAddress(), null));
                if (serverInfo.getPublicAddress() != null) {
                    map.put("public_address", converter.toConfig(InetSocketAddress.class, serverInfo.getPublicAddress(), null));
                }
            } catch (Exception e) {
                throw new RuntimeException("ServerListConverter#toConfig converter.toConfig threw exception", e);
            }
            section.set(serverInfo.getServerName(), map);

        });

        return section;
    }

    @Override
    public Object fromConfig(Class<?> type, Object obj, ParameterizedType parameterizedType) throws RuntimeException {
        ConfigSection section = (ConfigSection) obj;
        Map<Object, Object> values = section.getValues(true);
        ServerList list = new ServerList();
        Converter converter = this.internalConverter.getConverter(ServerInfo.class);
        values.forEach((key, data) -> {
            Map map = (Map) data;
            String name = (String) key;
            map.put("name", key);
            try {
                list.putIfAbsent(name, (ServerInfo) converter.fromConfig(ServerInfo.class, map, null));
            } catch (Exception e) {
                throw new RuntimeException("Cannot parse server info for " + name, e);
            }

        });
        return list;
    }

    @Override
    public boolean supports(Class<?> type) {
        return ServerList.class.isAssignableFrom(type);
    }
}
