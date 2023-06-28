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

package dev.waterdog.waterdogpe.utils.config.serializer;

import dev.waterdog.waterdogpe.network.serverinfo.ServerInfoType;
import dev.waterdog.waterdogpe.utils.config.ServerEntry;
import net.cubespace.Yamler.Config.ConfigSection;
import net.cubespace.Yamler.Config.Converter.Converter;
import net.cubespace.Yamler.Config.InternalConverter;

import java.lang.reflect.ParameterizedType;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public class ServerEntryConverter implements Converter {

    private final InternalConverter internalConverter;

    public ServerEntryConverter(InternalConverter internalConverter) {
        this.internalConverter = internalConverter;
    }

    @Override
    public Object toConfig(Class<?> type, Object object, ParameterizedType parameterizedType) {
        Map<String, String> map = new HashMap<>();
        ServerEntry serverEntry = (ServerEntry) object;
        map.put("address", serverEntry.getAddress().getAddress().getHostAddress() + ":" + serverEntry.getAddress().getPort());
        if (serverEntry.getPublicAddress() != null && serverEntry.getPublicAddress() != serverEntry.getAddress()) {
            map.put("public_address", serverEntry.getAddress().getAddress().getHostAddress() + ":" + serverEntry.getAddress().getPort());
        }
        if (serverEntry.getServerType() != null) {
            map.put("server_type", serverEntry.getServerType().toString());
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
        String serverType;

        if (object instanceof ConfigSection) {
            ConfigSection section = (ConfigSection) object;
            address = (InetSocketAddress) inetConverter.fromConfig(InetSocketAddress.class, section.get("address"), null);
            publicAddress = (InetSocketAddress) inetConverter.fromConfig(InetSocketAddress.class, section.get("public_address"), null);
            serverType = (String) inetConverter.fromConfig(String.class, section.get("server_type"), null);
            return new ServerEntry(section.get("name"), address, publicAddress, this.validateServerType(serverType));
        }

        if (object instanceof Map) {
            Map<String, Map<String, ?>> map = (Map<String, Map<String, ?>>) object;
            for (Map.Entry<String, Map<String, ?>> subMap : map.entrySet()) {
                address = (InetSocketAddress) inetConverter.fromConfig(InetSocketAddress.class, subMap.getValue().get("address"), null);
                publicAddress = (InetSocketAddress) inetConverter.fromConfig(InetSocketAddress.class, subMap.getValue().get("public_address"), null);
                serverType = (String) subMap.getValue().get("server_type");
                return new ServerEntry(subMap.getKey(), address, publicAddress, this.validateServerType(serverType));
            }
        }
        throw new IllegalArgumentException("ServerInfoConverter#fromConfig cannot parse obj: " + object.getClass().getName());
    }

    private String validateServerType(String serverType) {
        if (serverType == null || serverType.isEmpty()) {
            return ServerInfoType.BEDROCK.getIdentifier();
        }
        return serverType;
    }

    @Override
    public boolean supports(Class<?> type) {
        return ServerEntry.class.isAssignableFrom(type);
    }
}
