/*
 * Copyright 2021 WaterdogTEAM
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

package dev.waterdog.waterdogpe.network.serverinfo;

import dev.waterdog.waterdogpe.utils.config.ServerEntry;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * This class holds runtime information about avaliable ServerInfo instances and known implementations.
 * Custom ServerInfo implementations should be registered by providing ServerInfoType and ServerInfoFactory.
 * Plugins should do this using onStartup() method which is triggered before serialization from the configuration file.
 */
public class ServerInfoMap {

    public static final ServerInfoType DEFAULT_TYPE = ServerInfoType.BEDROCK;
    public static final ServerInfoFactory DEFAULT_FACTORY = BedrockServerInfo::new;

    private final Map<ServerInfoType, ServerInfoFactory> serverInfoTypes = new HashMap<>();
    private final Map<String, ServerInfo> serverList = new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER);

    public ServerInfoMap() {
        this.registerServerInfoFactory(DEFAULT_TYPE, DEFAULT_FACTORY);
    }

    public ServerInfo get(String name) {
        return this.serverList.get(name);
    }

    public ServerInfo putIfAbsent(String name, ServerInfo info) {
        return this.serverList.putIfAbsent(name, info);
    }

    public ServerInfo remove(String name) {
        return this.serverList.remove(name);
    }

    public ServerInfo put(String name, ServerInfo info) {
        return this.serverList.put(name, info);
    }

    public Collection<ServerInfo> values() {
        return Collections.unmodifiableCollection(this.serverList.values());
    }

    /**
     * To create new ServerInfo instance this method should be used
     * @param serverName name of the server
     * @param address address used to access the server
     * @param publicAddress address which can accessed from upstream session or null
     * @param serverType ServerInfoType which refers to ServerInfoFactory
     * @return new instance of ServerInfo
     */
    public ServerInfo createServerInfo(String serverName, InetSocketAddress address, InetSocketAddress publicAddress, ServerInfoType serverType) {
        ServerInfoFactory factory = this.serverInfoTypes.get(serverType);
        if (factory == null) {
            factory = DEFAULT_FACTORY;
        }
        return factory.newInstance(serverName, address, publicAddress);
    }

    public ServerInfo fromServerEntry(ServerEntry entry) {
        return this.createServerInfo(entry.getServerName(), entry.getAddress(), entry.getPublicAddress(), entry.getServerType());
    }

    /**
     * Allows to register custom ServerInfo class which can be used to serialize ServerInfo from config.
     * @param serverType instance of ServerInfoType
     * @param factory factory used to create serverInfo
     */
    public void registerServerInfoFactory(ServerInfoType serverType, ServerInfoFactory factory) {
        this.serverInfoTypes.put(serverType, factory);
    }

    public ServerInfoFactory getServerInfoFactory(ServerInfoType serverType) {
        return this.serverInfoTypes.get(serverType);
    }

    /**
     * Remove ServerInfo type from ServerInfoMap
     * @param serverType instance of ServerInfoType
     */
    public void removeServerInfoType(ServerInfoType serverType) {
        this.serverInfoTypes.remove(serverType);
    }
}
