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

package pe.waterdog.player;

import com.nukkitx.protocol.bedrock.*;
import com.nukkitx.protocol.bedrock.handler.BedrockPacketHandler;
import com.nukkitx.protocol.bedrock.packet.LoginPacket;
import pe.waterdog.ProxyServer;
import pe.waterdog.logger.Logger;
import pe.waterdog.network.bridge.ProxyBatchBridge;
import pe.waterdog.network.bridge.TransferBatchBridge;
import pe.waterdog.network.downstream.ConnectedDownstreamHandler;
import pe.waterdog.network.downstream.SwitchDownstreamHandler;
import pe.waterdog.network.downstream.InitialHandler;
import pe.waterdog.network.ServerInfo;
import pe.waterdog.network.entitymap.EntityMap;
import pe.waterdog.network.protocol.ProtocolConstants;
import pe.waterdog.network.session.LoginData;
import pe.waterdog.network.session.RewriteData;
import pe.waterdog.network.upstream.UpstreamHandler;

import java.security.KeyPair;
import java.util.*;

public class ProxiedPlayer {

    private final ProxyServer server;
    private final ProtocolConstants.Protocol protocol;

    private BedrockClient client;
    private BedrockClient pendingConnection = null;
    private final BedrockServerSession session;
    private BedrockClientSession downstreamConnection;

    private final KeyPair keyPair;
    private final LoginPacket loginPacket;
    private final LoginData loginData;

    private final RewriteData rewriteData = new RewriteData();
    private boolean canRewrite = false;

    private final EntityMap entityMap;
    private ServerInfo serverInfo;

    private List<Long> entities;
    private List<Long> players;

    private boolean dimensionChange = false;

    public ProxiedPlayer(ProxyServer server, BedrockServerSession session, ProtocolConstants.Protocol protocol, KeyPair keyPair, LoginPacket loginPacket, LoginData loginData){
        this.server = server;
        this.session = session;
        this.protocol = protocol;
        this.keyPair = keyPair;
        this.loginPacket = loginPacket;
        this.loginData = loginData;
        this.entityMap = new EntityMap(this);
    }

    public void initialConnect(){
        //TODO: login event
        //TODO: get server from handler
        this.session.setPacketHandler(new UpstreamHandler(this, this.session));
        this.session.addDisconnectHandler((reason) -> this.disconnect());

        String server = this.server.getConfiguration().getPriorities().get(0);
        this.connect(this.server.getServer(server));
    }

    public void connect(ServerInfo serverInfo){
        if (serverInfo == null) return;

        if (this.serverInfo == serverInfo){ //Already connected
            return;
        }

        final BedrockPacketHandler handler;
        if (this.downstreamConnection == null){
            handler = new InitialHandler(this);
            this.serverInfo = serverInfo;
        }else {
            handler = null;
        }

        //TODO: ServerSwitch event

        BedrockClient client = this.server.getPlayerManager().bindClient();
        client.connect(serverInfo.getAddress()).whenComplete((downstream, throwable)->{
            if (throwable != null){
                //TODO: remove this debug
                this.getLogger().error("["+this.session.getAddress()+"|"+this.getName()+"] Unable to connect to downstream "+serverInfo.getServerName(), throwable);

                //TODO: fallback listener
                this.pendingConnection = null;
                return;
            }

            downstream.setPacketCodec(this.protocol.getCodec());

            if (this.downstreamConnection == null){
                this.client = client;
                this.downstreamConnection = downstream;
                this.session.setBatchHandler(new ProxyBatchBridge(this, downstream));
                downstream.setBatchHandler(new ProxyBatchBridge(this, this.session));
            }else {
                //Server switch
                this.pendingConnection = client;
                downstream.setBatchHandler(new TransferBatchBridge(this, this.session));
            }

            downstream.setPacketHandler(handler == null? new SwitchDownstreamHandler(this, serverInfo, downstream) : handler);
            downstream.sendPacketImmediately(this.loginPacket);
            downstream.setLogging(true);

            downstream.addDisconnectHandler((reason) -> {
                this.getLogger().info("["+downstream.getAddress()+"|"+this.getName()+"] -> Downstream ["+serverInfo.getServerName()+"] has disconnected");
            });
            this.session.addDisconnectHandler(reason -> {
                if (!downstream.isClosed()) downstream.disconnect();
            });
            this.getLogger().info("["+this.session.getAddress()+"|"+this.getName()+"] -> Downstream ["+serverInfo.getServerName()+"] has connected");
        });

    }

    public void transferServer(ServerInfo serverInfo){
        if (this.pendingConnection == null){
            //TODO: error
            System.out.println("error");
            return;
        }

        this.closeDownstream();
        this.serverInfo = serverInfo;

        this.client = this.pendingConnection;
        this.downstreamConnection = this.pendingConnection.getSession();

        this.session.setBatchHandler(new ProxyBatchBridge(this, this.downstreamConnection));
        this.downstreamConnection.setBatchHandler(new ProxyBatchBridge(this, this.session));
        this.downstreamConnection.setPacketHandler(new ConnectedDownstreamHandler(this, this.downstreamConnection));
    }

    public void closeDownstream(){
        if (this.downstreamConnection == null || this.downstreamConnection.isClosed()){
            return;
        }

        this.downstreamConnection.disconnect();
        this.client.close();
    }

    public void disconnect(){
        this.disconnect(null);
    }

    public void disconnect(String reason){
        //TODO: disconnect event

        if (this.session != null && !this.session.isClosed()){
            this.client.close();
        }

        this.server.getPlayerManager().removePlayer(this);
        this.getLogger().info("["+this.getName()+"] -> Upstream has disconnected");
        if (reason != null) this.getLogger().info("["+this.getName()+"] -> Disconnected with: §c" + reason);
    }

    public void putPacket(BedrockPacket packet){
        if (this.session == null || this.session.isClosed()) return;
        this.session.sendPacket(packet);
    }

    public ServerInfo getServerInfo() {
        return this.serverInfo;
    }

    public BedrockServerSession getUpstream() {
        return this.session;
    }

    /**
     * Alias to getConnection()
     */
    public BedrockClientSession getDownstream() {
        return this.downstreamConnection;
    }

    public BedrockClient getPendingConnection() {
        return this.pendingConnection;
    }

    public EntityMap getEntityMap() {
        return entityMap;
    }

    public UUID getUniqueId() {
        return this.loginData.getUuid();
    }

    public String getXuid() {
        return this.loginData.getXuid();
    }

    public KeyPair getKeyPair() {
        return keyPair;
    }

    public String getName() {
        return this.loginData.getDisplayName();
    }

    public RewriteData getRewriteData() {
        return this.rewriteData;
    }

    public Logger getLogger() {
        return this.server.getLogger();
    }

    public void setDimensionChange(boolean dimensionChange) {
        this.dimensionChange = dimensionChange;
    }

    public boolean isDimensionChange() {
        return this.dimensionChange;
    }

    public void setCanRewrite(boolean canRewrite) {
        this.canRewrite = canRewrite;
    }

    public boolean canRewrite() {
        return this.canRewrite;
    }
}
