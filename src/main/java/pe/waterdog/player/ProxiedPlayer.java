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

import com.nukkitx.protocol.bedrock.BedrockClient;
import com.nukkitx.protocol.bedrock.BedrockClientSession;
import com.nukkitx.protocol.bedrock.BedrockPacket;
import com.nukkitx.protocol.bedrock.BedrockServerSession;
import com.nukkitx.protocol.bedrock.handler.BedrockPacketHandler;
import com.nukkitx.protocol.bedrock.packet.LoginPacket;
import pe.waterdog.ProxyServer;
import pe.waterdog.logger.Logger;
import pe.waterdog.network.bridge.ProxyBatchBridge;
import pe.waterdog.network.bridge.ProxyBatchTransferBridge;
import pe.waterdog.network.downstream.ConnectedHandler;
import pe.waterdog.network.downstream.InitialHandler;
import pe.waterdog.network.ServerInfo;
import pe.waterdog.network.entitymap.EntityMap;
import pe.waterdog.network.protocol.ProtocolConstants;
import pe.waterdog.network.session.LoginData;

import java.security.KeyPair;
import java.util.List;
import java.util.UUID;

public class ProxiedPlayer {

    private final ProxyServer server;
    private final ProtocolConstants.Protocol protocol;

    private BedrockClient client;
    private final BedrockServerSession session;
    private BedrockClientSession connection;
    private BedrockClientSession pendingConnection;

    private final KeyPair keyPair;
    private final LoginPacket loginPacket;
    private final LoginData loginData;
    private PlayerRewriteData rewriteData;

    private final EntityMap entityMap;
    private ServerInfo serverInfo;

    private List<Long> entities;
    private List<Long> players;

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
        this.session.addDisconnectHandler((reason) -> {
            this.disconnect();
        });


        String server = this.server.getConfiguration().getPriorities().get(0);
        this.connect(this.server.getServer(server));
    }

    public void connect(ServerInfo serverInfo){
        if (serverInfo == null) return;

        if (this.serverInfo == serverInfo){
            //Already connected
            return;
        }


        final BedrockPacketHandler handler;
        if (this.connection == null){
            handler = new InitialHandler(this);
            this.serverInfo = serverInfo;
        }else {
            handler = new ConnectedHandler(this, serverInfo);

        }

        //TODO: ServerSwitch event

        this.client = this.server.getPlayerManager().bindClient();
        client.connect(serverInfo.getAddress()).whenComplete((downstream, throwable)->{
            if (throwable != null){
                //TODO: remove this debug
                this.getLogger().error("["+this.session.getAddress()+"|"+this.getName()+"] Unable to connect to downstream "+serverInfo.getServerName(), throwable);

                //TODO: fallback listener
                this.pendingConnection = null;
                return;
            }

            downstream.setPacketCodec(this.protocol.getCodec());

            if (this.connection == null){
                this.connection = downstream;
                this.session.setBatchedHandler(new ProxyBatchBridge(this, downstream));
                downstream.setBatchedHandler(new ProxyBatchBridge(this, this.session));
            }else {
                //Server switch
                this.pendingConnection = downstream;
                downstream.setBatchedHandler(new ProxyBatchTransferBridge());
            }

            downstream.setPacketHandler(handler);
            downstream.sendPacketImmediately(this.loginPacket);
            downstream.setLogging(true);

            downstream.addDisconnectHandler((reason) -> {
                this.getLogger().info("["+downstream.getAddress()+"|"+this.getName()+"] -> Downstream ["+serverInfo.getServerName()+"] has disconnected");
            });
            this.session.addDisconnectHandler((reason) -> {
                if (!downstream.isClosed()) downstream.disconnect();
            });

            this.getLogger().info("["+this.session.getAddress()+"|"+this.getName()+"] -> Downstream ["+serverInfo.getServerName()+"] has connected");
        });

    }

    public void finishTransfer(ServerInfo serverInfo){
        if (this.pendingConnection == null || this.pendingConnection.isClosed()) return;
        this.serverInfo = serverInfo;

        this.connection.disconnect();
        this.connection = this.pendingConnection;

        this.pendingConnection = null;

        this.session.setBatchedHandler(new ProxyBatchBridge(this, this.connection));
        this.connection.setBatchedHandler(new ProxyBatchBridge(this, this.session));
    }

    public void disconnect(){
        this.disconnect(null);
    }

    public void disconnect(String reason){
        //TODO: disconnect event

        if (!this.session.isClosed()){
            this.session.disconnect(reason);
        }

        this.server.getPlayerManager().removePlayer(this);
        this.getLogger().info("["+this.session.getAddress()+"|"+this.getName()+"] -> Upstream has disconnected");
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

    public BedrockClientSession getConnection() {
        return this.connection;
    }

    /**
     * Alias to getConnection()
     */
    public BedrockClientSession getDownstream() {
        return this.connection;
    }

    public BedrockClientSession getPendingConnection() {
        return this.pendingConnection;
    }

    public void setPendingConnection(BedrockClientSession pendingConnection) {
        this.pendingConnection = pendingConnection;
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

    public void setRewriteData(PlayerRewriteData rewriteData) {
        this.rewriteData = rewriteData;
    }

    public PlayerRewriteData getRewriteData() {
        return this.rewriteData;
    }

    public Logger getLogger() {
        return this.server.getLogger();
    }
}
