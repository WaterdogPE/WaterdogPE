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

import com.nimbusds.jwt.SignedJWT;
import com.nukkitx.network.raknet.EncapsulatedPacket;
import com.nukkitx.protocol.bedrock.BedrockClient;
import com.nukkitx.protocol.bedrock.BedrockPacket;
import com.nukkitx.protocol.bedrock.BedrockServerSession;
import com.nukkitx.protocol.bedrock.handler.BedrockPacketHandler;
import com.nukkitx.protocol.bedrock.packet.ClientToServerHandshakePacket;
import com.nukkitx.protocol.bedrock.packet.LoginPacket;
import com.nukkitx.protocol.bedrock.util.EncryptionUtils;
import pe.waterdog.ProxyServer;
import pe.waterdog.logger.Logger;
import pe.waterdog.network.ProxyBatchBridge;
import pe.waterdog.network.downstream.ConnectedHandler;
import pe.waterdog.network.downstream.InitialHandler;
import pe.waterdog.network.downstream.ServerInfo;
import pe.waterdog.network.protocol.ProtocolConstants;
import pe.waterdog.network.session.LoginData;
import pe.waterdog.network.upstream.UpstreamHandler;
import pe.waterdog.utils.PlayerRewriteData;

import javax.crypto.SecretKey;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.KeyPair;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;
import java.util.UUID;

public class ProxiedPlayer {

    private final ProxyServer server;
    private final ProtocolConstants.Protocol protocol;
    private final BedrockServerSession session;

    private final KeyPair keyPair;
    private final LoginPacket loginPacket;
    private final LoginData loginData;
    private PlayerRewriteData rewriteData;

    private BedrockClient client;
    private ServerInfo connection;

    public ProxiedPlayer(ProxyServer server, BedrockServerSession session, ProtocolConstants.Protocol protocol, KeyPair keyPair, LoginPacket loginPacket, LoginData loginData){
        this.server = server;
        this.session = session;
        this.protocol = protocol;
        this.keyPair = keyPair;
        this.loginPacket = loginPacket;
        this.loginData = loginData;
    }

    public void initialConnect(){
        //TODO: login event
        //TODO: get server from handler
        this.session.setPacketHandler(new UpstreamHandler(this, this.session));
        this.session.addDisconnectHandler((reason) -> {
            this.disconnect();
        });

        ServerInfo serverInfo = new ServerInfo("lobby", new InetSocketAddress("192.168.0.50", 19134));
        this.connect(serverInfo);
    }

    public void connect(ServerInfo serverInfo){
        ServerInfo connection = new ServerInfo(serverInfo.getServerName(), serverInfo.getAddress());
        final BedrockPacketHandler handler;

        //First connection
        if (this.connection == null){
            handler = new InitialHandler(this);
        }else {
            handler = new ConnectedHandler(this);
        }

        //TODO: ServerSwitch event

        this.client = this.server.getPlayerManager().bindClient();
        this.client.connect(connection.getAddress()).whenComplete((downstream, throwable)->{
            if (throwable != null){
                this.getLogger().error("["+this.session.getAddress()+"|"+this.getName()+"] Unable to connect to downstream "+serverInfo.getServerName(), throwable);
                return;
            }

            if (this.connection != null){
                this.connection.getDownstreamClient().disconnect();
            }

            downstream.setPacketCodec(this.protocol.getCodec());
            this.session.setBatchedHandler(new ProxyBatchBridge(downstream));

            this.connection = connection;
            this.connection.setDownstreamClient(downstream);

            downstream.setPacketHandler(handler);
            downstream.setBatchedHandler(new ProxyBatchBridge(this.session));
            downstream.sendPacketImmediately(this.loginPacket);
            downstream.setLogging(true);
            this.session.addDisconnectHandler((reason) -> {
                if (!downstream.isClosed()) downstream.disconnect();
            });

            this.getLogger().info("["+this.session.getAddress()+"|"+this.getName()+"] -> Downstream ["+serverInfo.getServerName()+"] has connected");
        });

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
        if (this.session == null) return;

        this.session.sendPacket(packet);
    }

    public ServerInfo getConnection() {
        return connection;
    }

    public BedrockServerSession getUpstream() {
        return this.session;
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
