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

import com.google.common.base.Preconditions;
import com.nukkitx.protocol.bedrock.*;
import com.nukkitx.protocol.bedrock.packet.LoginPacket;
import io.netty.util.AsciiString;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.*;
import pe.waterdog.ProxyServer;
import pe.waterdog.logger.Logger;
import pe.waterdog.network.bridge.DownstreamBridge;
import pe.waterdog.network.bridge.ProxyBatchBridge;
import pe.waterdog.network.bridge.TransferBatchBridge;
import pe.waterdog.network.downstream.SwitchDownstreamHandler;
import pe.waterdog.network.downstream.InitialHandler;
import pe.waterdog.network.ServerInfo;
import pe.waterdog.network.rewrite.BlockMap;
import pe.waterdog.network.rewrite.EntityMap;
import pe.waterdog.network.protocol.ProtocolConstants;
import pe.waterdog.network.rewrite.EntityTracker;
import pe.waterdog.network.session.LoginData;
import pe.waterdog.network.rewrite.types.RewriteData;
import pe.waterdog.network.session.ServerConnection;
import pe.waterdog.network.session.SessionInjections;
import pe.waterdog.network.upstream.UpstreamHandler;

import java.security.KeyPair;
import java.util.*;

public class ProxiedPlayer {

    private final ProxyServer proxy;

    private final BedrockServerSession upstream;
    private ServerConnection serverConnection;

    private ServerInfo pendingConnection;

    private LoginPacket loginPacket;
    private final LoginData loginData;

    private final RewriteData rewriteData = new RewriteData();
    private boolean canRewrite = false;

    private final EntityTracker entityTracker;

    private final EntityMap entityMap;
    private final BlockMap blockMap;

    private final LongSet entities = new LongOpenHashSet();
    private final Collection<UUID> players = new HashSet<>();
    private final ObjectSet<String> scoreboards = new ObjectOpenHashSet<>();

    private boolean dimensionChange = false;

    public ProxiedPlayer(ProxyServer proxy, BedrockServerSession session, LoginData loginData){
        this.proxy = proxy;
        this.upstream = session;
        this.loginData = loginData;
        this.loginPacket = loginData.constructLoginPacket();
        this.entityTracker = new EntityTracker(this);
        this.entityMap = new EntityMap(this);
        this.blockMap = new BlockMap(this);
    }

    public void initialConnect(){
        //TODO: login event
        //TODO: get server from handler
        this.upstream.setPacketHandler(new UpstreamHandler(this));
        this.upstream.addDisconnectHandler((reason) -> this.disconnect(null, true));

        String server = this.proxy.getConfiguration().getPriorities().get(0);
        this.connect(this.proxy.getServer(server));
    }

    public void connect(ServerInfo serverInfo){
        Preconditions.checkNotNull(serverInfo, "Server info can not be null!");

        //TODO: ServerSwitch event

        if (this.serverConnection != null && this.serverConnection.getInfo() == serverInfo){
            //Already connected
            return;
        }

        if (this.pendingConnection == serverInfo){
            return;
        }

        BedrockClient client = this.proxy.getPlayerManager().bindClient();
        client.connect(serverInfo.getAddress()).whenComplete((downstream, throwable)->{
            if (throwable != null){
                this.getLogger().error("["+this.upstream.getAddress()+"|"+this.getName()+"] Unable to connect to downstream "+serverInfo.getServerName(), throwable);

                //TODO: fallback listener
                this.pendingConnection = null;
                return;
            }

            if (this.serverConnection == null) {
                this.serverConnection = new ServerConnection(client, downstream, serverInfo);

                downstream.setPacketHandler(new InitialHandler(this));
                downstream.setBatchHandler(new DownstreamBridge(this, this.upstream));
                this.upstream.setBatchHandler(new ProxyBatchBridge(this, downstream));
            }else {
                this.pendingConnection = serverInfo;

                downstream.setPacketHandler(new SwitchDownstreamHandler(this, serverInfo, client));
                downstream.setBatchHandler(new TransferBatchBridge(this, this.upstream));
            }

            downstream.setPacketCodec(this.getProtocol().getCodec());
            downstream.sendPacketImmediately(this.loginPacket);
            downstream.setLogging(true);

            SessionInjections.injectNewDownstream(downstream, this, serverInfo);
            this.getLogger().info("["+this.upstream.getAddress()+"|"+this.getName()+"] -> Downstream ["+serverInfo.getServerName()+"] has connected");
        });

    }

    public void disconnect(){
        this.disconnect(null, false);
    }

    public void disconnect(String reason){
        this.disconnect(reason, false);
    }

    public void disconnect(String reason, boolean force){
        //TODO: disconnect event

        if (this.upstream != null && !this.upstream.isClosed()){
            this.upstream.disconnect(reason);
        }

        if (this.serverConnection != null){
            this.serverConnection.getInfo().removePlayer(this);
            this.serverConnection.disconnect();
        }

        this.proxy.getPlayerManager().removePlayer(this);
        this.getLogger().info("["+this.getName()+"] -> Upstream has disconnected");
        if (reason != null) this.getLogger().info("["+this.getName()+"] -> Disconnected with: §c" + reason);
    }

    public void sendPacket(BedrockPacket packet){
        if (this.upstream != null && !this.upstream.isClosed()){
            this.upstream.sendPacket(packet);
        }
    }

    public ProxyServer getProxy() {
        return this.proxy;
    }

    public Logger getLogger() {
        return this.proxy.getLogger();
    }

    public ServerConnection getServer() {
        return this.serverConnection;
    }

    public void setServer(ServerConnection serverConnection) {
        this.serverConnection = serverConnection;
    }

    public ServerInfo getPendingConnection() {
        return this.pendingConnection;
    }

    public void setPendingConnection(ServerInfo pendingConnection) {
        this.pendingConnection = pendingConnection;
    }

    public BedrockServerSession getUpstream() {
        return this.upstream;
    }

    public EntityTracker getEntityTracker() {
        return this.entityTracker;
    }

    public EntityMap getEntityMap() {
        return this.entityMap;
    }

    public BlockMap getBlockMap() {
        return this.blockMap;
    }

    public LoginData getLoginData() {
        return this.loginData;
    }

    public String getName() {
        return this.loginData.getDisplayName();
    }

    public UUID getUniqueId() {
        return this.loginData.getUuid();
    }

    public String getXuid() {
        return this.loginData.getXuid();
    }

    public ProtocolConstants.Protocol getProtocol() {
        return this.loginData.getProtocol();
    }

    public RewriteData getRewriteData() {
        return this.rewriteData;
    }

    public void setCanRewrite(boolean canRewrite) {
        this.canRewrite = canRewrite;
    }

    public boolean canRewrite() {
        return this.canRewrite;
    }

    public LongSet getEntities() {
        return this.entities;
    }

    public Collection<UUID> getPlayers() {
        return this.players;
    }

    public ObjectSet<String> getScoreboards() {
        return this.scoreboards;
    }
}
