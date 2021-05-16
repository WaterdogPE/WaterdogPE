package dev.waterdog.waterdogpe.event.defaults;

import dev.waterdog.waterdogpe.event.Event;
import dev.waterdog.waterdogpe.network.ServerInfo;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;

public class FastTransferRequestEvent extends Event {
    private ServerInfo serverInfo;
    private ProxiedPlayer player;
    private String address;
    private int port;

    public FastTransferRequestEvent(ServerInfo serverInfo, ProxiedPlayer player, String address, int port) {
        this.serverInfo = serverInfo;
        this.player = player;
        this.address = address;
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public ProxiedPlayer getPlayer() {
        return player;
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

    public void setPlayer(ProxiedPlayer player) {
        this.player = player;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setServerInfo(ServerInfo serverInfo) {
        this.serverInfo = serverInfo;
    }
}
