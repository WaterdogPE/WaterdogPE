package dev.waterdog.waterdogpe.event.defaults;

import dev.waterdog.waterdogpe.network.ServerInfo;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;


public class FastTransferRequestEvent extends PlayerEvent {
    private ServerInfo serverInfo;
    private String address;
    private int port;

    public FastTransferRequestEvent(ServerInfo serverInfo, ProxiedPlayer player, String address, int port) {
        super(player);
        this.serverInfo = serverInfo;
        this.address = address;
        this.port = port;
    }

    public int getPort() {
        return port;
    }


    public ServerInfo getServerInfo() {
        return serverInfo;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setServerInfo(ServerInfo serverInfo) {
        this.serverInfo = serverInfo;
    }
}
