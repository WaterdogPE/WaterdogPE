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

import dev.waterdog.waterdogpe.utils.config.ServerEntry;
import dev.waterdog.waterdogpe.utils.config.ServerList;
import net.cubespace.Yamler.Config.ConfigSection;
import net.cubespace.Yamler.Config.Converter.Converter;
import net.cubespace.Yamler.Config.InternalConverter;

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
    public Object toConfig(Class<?> type, Object object, ParameterizedType parameterizedType) throws RuntimeException {
        ServerList list = (ServerList) object;
        ConfigSection section = new ConfigSection();
        Converter converter = internalConverter.getConverter(InetSocketAddress.class);

        for (ServerEntry serverEntry : list.values()) {
            Map<String, Object> map = new HashMap<>();
            try {
                map.put("address", converter.toConfig(InetSocketAddress.class, serverEntry.getAddress(), null));
                if (serverEntry.getPublicAddress() != null) {
                    map.put("public_address", converter.toConfig(InetSocketAddress.class, serverEntry.getPublicAddress(), null));
                }
                if (serverEntry.getServerType() != null) {
                    map.put("server_type", serverEntry.getServerType().toString());
                }
            } catch (Exception e) {
                throw new RuntimeException("ServerListConverter#toConfig converter.toConfig threw exception", e);
            }
            section.set(serverEntry.getServerName(), map);
        }
        return section;
    }

    @Override
    public Object fromConfig(Class<?> type, Object object, ParameterizedType parameterizedType) throws RuntimeException {
        ConfigSection section = (ConfigSection) object;
        Map<Object, Object> values = section.getValues(true);
        ServerList list = new ServerList();
        Converter converter = this.internalConverter.getConverter(ServerEntry.class);

        for (Map.Entry<Object, Object> entry : values.entrySet()) {
            Map<String, Object> map = (Map<String, Object>) entry.getValue();
            String name = (String) entry.getKey();
            try {
                Map<String, Object> entryMap = new HashMap<>();
                entryMap.put(name, map);
                list.putIfAbsent(name, (ServerEntry) converter.fromConfig(ServerEntry.class, entryMap, null));
            } catch (Exception e) {
                throw new RuntimeException("Cannot parse server info for " + name, e);
            }
        }
        return list;
    }

    @Override
    public boolean supports(Class<?> type) {
        return ServerList.class.isAssignableFrom(type);
    }
}
