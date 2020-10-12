/**
 * Copyright 2020 WaterdogTEAM
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pe.waterdog.utils;

import pe.waterdog.logger.MainLogger;
import pe.waterdog.network.ServerInfo;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProxyConfig extends YamlConfig {

    private String motd;
    private int maxPlayerCount;

    private final boolean onlineMode;
    private final boolean replaceUsernameSpaces;
    private boolean useLoginExtras;
    private boolean ipForward;

    private final InetSocketAddress bindAddress;
    private final List<String> priorities;
    private final Map<String, InetSocketAddress> servers;
    private Map<String, String> forcedHosts;

    private final Map<String, List<String>> playerPermissions = new HashMap<>();
    private List<String> defaultPermissions;

    public ProxyConfig(File file){
        super(file);

        this.motd = this.getString("listener.motd");
        this.maxPlayerCount = this.getInt("listener.max_players");
        this.useLoginExtras = this.getBoolean("use_login_extras");
        this.onlineMode = this.getBoolean("online_mode");
        this.ipForward = this.getBoolean("ip_forward");
        this.replaceUsernameSpaces = this.getBoolean("replace_username_spaces");
        this.bindAddress = this.getInetAddress("listener.host");
        this.priorities = this.getStringList("listener.priorities");
        this.servers = this.getInetAddressMap("servers");
        this.defaultPermissions = this.getStringList("permissions_default");
        this.playerPermissions.putAll(this.getPlayerPermissions("permissions"));
    }

    public InetSocketAddress getInetAddress(String key) {
        String addressString = this.getString(key);
        if (addressString == null) return null;

        String[] data = addressString.split(":");
        return new InetSocketAddress(data[0], (data.length <= 1 ? 19132 : Integer.parseInt(data[1])));
    }

    private Map<String, InetSocketAddress> getInetAddressMap(String key) {
        Map<String, Map<String, String>> map = (Map<String, Map<String, String>>) this.get(key);
        Map<String, InetSocketAddress> servers = new HashMap<>();

        for (String server : map.keySet()) {
            InetSocketAddress address = null;
            try {
                String[] data = map.get(server).get("address").split(":");
                address = new InetSocketAddress(data[0], Integer.parseInt(data[1]));
            } catch (Exception e) {
                MainLogger.getLogger().error("Unable to parse server from config! Please check you configuration. Server name: " + server);
            }

            if (address != null) servers.put(server, address);
        }
        return servers;
    }

    public Map<String, ServerInfo> buildServerMap() {
        Map<String, ServerInfo> servers = new HashMap<>();
        for (Map.Entry<String, InetSocketAddress> entry : this.servers.entrySet()) {
            servers.put(entry.getKey().toLowerCase(), new ServerInfo(entry.getKey().toLowerCase(), entry.getValue()));
        }
        return servers;
    }

    private Map<String, List<String>> getPlayerPermissions(String key){
        return (Map<String, List<String>>) this.get(key);
    }

    public String getMotd() {
        return this.motd;
    }

    public void setMotd(String motd) {
        this.motd = motd;
    }

    public int getMaxPlayerCount() {
        return this.maxPlayerCount;
    }

    public void setMaxPlayerCount(int maxPlayerCount) {
        this.maxPlayerCount = maxPlayerCount;
    }

    public boolean isOnlineMode() {
        return this.onlineMode;
    }

    public boolean useLoginExtras() {
        return this.useLoginExtras;
    }

    public void setUseLoginExtras(boolean useLoginExtras) {
        this.useLoginExtras = useLoginExtras;
    }

    public boolean isReplaceUsernameSpaces() {
        return this.replaceUsernameSpaces;
    }

    public boolean isIpForward() {
        return this.ipForward;
    }

    public void setIpForward(boolean ipForward) {
        this.ipForward = ipForward;
    }

    public InetSocketAddress getBindAddress() {
        return this.bindAddress;
    }

    public List<String> getPriorities() {
        return this.priorities;
    }

    public Map<String, InetSocketAddress> getServers() {
        return this.servers;
    }

    public Map<String, String> getForcedHosts() {
        return this.forcedHosts;
    }

    public Map<String, List<String>> getPlayerPermissions() {
        return this.playerPermissions;
    }

    public void setDefaultPermissions(List<String> defaultPermissions) {
        this.defaultPermissions = defaultPermissions;
    }

    public List<String> getDefaultPermissions() {
        return this.defaultPermissions;
    }
}
