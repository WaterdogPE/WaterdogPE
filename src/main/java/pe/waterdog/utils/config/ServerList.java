package pe.waterdog.utils.config;

import pe.waterdog.network.ServerInfo;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashMap;

/**
 * This is a wrapper class for a map mapping all the server names to corresponding ServerInfo instances.
 * This class is required for configuration auto parsing
 */
public class ServerList {

    private final HashMap<String, ServerInfo> serverList = new HashMap<>();

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
        return this.serverList.values();
    }

    public ServerList initEmpty() {
        this.putIfAbsent("lobby1", new ServerInfo("lobby1",
                new InetSocketAddress("127.0.0.1", 19133),
                new InetSocketAddress("play.myserver.com", 19133)));
        return this;
    }
}
