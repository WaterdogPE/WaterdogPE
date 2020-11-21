package pe.waterdog.utils.config;

import net.cubespace.Yamler.Config.Converter.Converter;
import net.cubespace.Yamler.Config.InternalConverter;

import java.lang.reflect.ParameterizedType;
import java.net.InetSocketAddress;

public class InetSocketAddressConverter implements Converter {

    private final InternalConverter internalConverter;

    public InetSocketAddressConverter(InternalConverter internalConverter) {
        this.internalConverter = internalConverter;
    }

    @Override
    public Object toConfig(Class<?> type, Object object, ParameterizedType parameterizedType) throws Exception {
        if (object == null){
            return null;
        }
        InetSocketAddress address = (InetSocketAddress) object;
        return address.getHostName() + ":" + address.getPort();
    }

    @Override
    public Object fromConfig(Class<?> type, Object object, ParameterizedType parameterizedType) throws Exception {
        if (object == null) {
            return null;
        }
        String string = (String) object;
        String[] parts = string.split(":");
        return new InetSocketAddress(parts[0], Integer.parseInt(parts[1]));
    }

    @Override
    public boolean supports(Class<?> type) {
        return InetSocketAddress.class.isAssignableFrom(type);
    }
}
