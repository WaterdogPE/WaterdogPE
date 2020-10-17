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
import com.google.common.base.Strings;
import com.nukkitx.protocol.bedrock.BedrockClient;
import com.nukkitx.protocol.bedrock.BedrockPacket;
import com.nukkitx.protocol.bedrock.BedrockServerSession;
import com.nukkitx.protocol.bedrock.packet.LoginPacket;
import com.nukkitx.protocol.bedrock.packet.SetTitlePacket;
import com.nukkitx.protocol.bedrock.packet.TextPacket;
import com.nukkitx.protocol.bedrock.packet.TransferPacket;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.*;
import lombok.NonNull;
import pe.waterdog.ProxyServer;
import pe.waterdog.command.CommandSender;
import pe.waterdog.event.defaults.PlayerDisconnectEvent;
import pe.waterdog.event.defaults.PlayerLoginEvent;
import pe.waterdog.event.defaults.PreTransferEvent;
import pe.waterdog.logger.MainLogger;
import pe.waterdog.network.bridge.UpstreamBridge;
import pe.waterdog.utils.types.PacketHandler;
import pe.waterdog.utils.types.Permission;
import pe.waterdog.utils.types.TextContainer;
import pe.waterdog.network.ServerInfo;
import pe.waterdog.network.bridge.DownstreamBridge;
import pe.waterdog.network.bridge.TransferBatchBridge;
import pe.waterdog.network.downstream.InitialHandler;
import pe.waterdog.network.downstream.SwitchDownstreamHandler;
import pe.waterdog.network.protocol.ProtocolConstants;
import pe.waterdog.network.rewrite.BlockMap;
import pe.waterdog.network.rewrite.EntityMap;
import pe.waterdog.network.rewrite.EntityTracker;
import pe.waterdog.network.rewrite.types.RewriteData;
import pe.waterdog.network.session.LoginData;
import pe.waterdog.network.session.ServerConnection;
import pe.waterdog.network.session.SessionInjections;
import pe.waterdog.utils.types.TranslationContainer;

import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ProxiedPlayer implements CommandSender {

    private final ProxyServer proxy;

    private final BedrockServerSession upstream;
    private ServerConnection serverConnection;
    private ServerInfo pendingConnection;

    private final RewriteData rewriteData = new RewriteData();
    private final LoginData loginData;
    private LoginPacket loginPacket;

    private final EntityTracker entityTracker;
    private final EntityMap entityMap;
    private final BlockMap blockMap;

    private final LongSet entities = new LongOpenHashSet();
    private final LongSet bossbars = new LongOpenHashSet();
    private final Collection<UUID> players = new HashSet<>();
    private final ObjectSet<String> scoreboards = new ObjectOpenHashSet<>();

    private final Object2ObjectMap<String, Permission> permissions = new Object2ObjectOpenHashMap<>();
    private boolean admin = false;

    private boolean canRewrite = false;
    private boolean acceptPlayStatus = false;

    /**
     * Additional downstream and upstream handlers can be set by plugin.
     * Do not set directly BedrockPacketHandler to sessions!
     */
    private PacketHandler pluginUpstreamHandler = null;
    private PacketHandler pluginDownstreamHandler = null;

    public ProxiedPlayer(ProxyServer proxy, BedrockServerSession session, LoginData loginData) {
        this.proxy = proxy;
        this.upstream = session;
        this.loginData = loginData;
        this.loginPacket = loginData.constructLoginPacket();
        this.entityTracker = new EntityTracker(this);
        this.entityMap = new EntityMap(this);
        this.blockMap = new BlockMap(this);
        this.proxy.getPlayerManager().subscribePermissions(this);
    }

    public void initialConnect() {
        PlayerLoginEvent event = new PlayerLoginEvent(this);
        this.proxy.getEventManager().callEvent(event).whenComplete((futureEvent, ignored) -> {
            if (event.isCancelled()) {
                this.disconnect(event.getCancelReason());
                return;
            }
            SessionInjections.injectUpstreamHandlers(this.upstream, this);

            ServerInfo serverInfo = this.getProxy().getJoinHandler().determineServer(this);
            if (serverInfo != null) {
                this.connect(serverInfo);
            } else {
                this.disconnect(new TranslationContainer("waterdog.no.initial.server").getTranslated());
            }
        });
    }

    public void connect(ServerInfo serverInfo) {
        Preconditions.checkNotNull(serverInfo, "Server info can not be null!");

        PreTransferEvent event = new PreTransferEvent(this, serverInfo);
        ProxyServer.getInstance().getEventManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }

        final @NonNull ServerInfo targetServer = event.getTargetServer();

        if (this.serverConnection != null && this.serverConnection.getInfo() == targetServer) {
            this.sendMessage(new TranslationContainer("waterdog.downstream.connected", serverInfo.getServerName()));
            return;
        }

        if (this.pendingConnection == targetServer) {
            this.sendMessage(new TranslationContainer("waterdog.downstream.connecting", serverInfo.getServerName()));
            return;
        }

        CompletableFuture<BedrockClient> future = this.proxy.bindClient(this.getProtocol());
        future.thenAccept(client -> client.connect(targetServer.getAddress()).whenComplete((downstream, throwable) -> {
            if (throwable != null) {
                this.getLogger().error("[" + this.upstream.getAddress() + "|" + this.getName() + "] Unable to connect to downstream " + targetServer.getServerName(), throwable);
                this.sendMessage(new TranslationContainer("waterdog.downstream.transfer.failed", serverInfo.getServerName(), throwable.getLocalizedMessage()));
                //TODO: reconnect handler
                this.pendingConnection = null;
                return;
            }

            if (this.serverConnection == null) {
                this.serverConnection = new ServerConnection(client, downstream, targetServer);
                targetServer.addPlayer(this);

                downstream.setPacketHandler(new InitialHandler(this));
                downstream.setBatchHandler(new DownstreamBridge(this, this.upstream));
                this.upstream.setBatchHandler(new UpstreamBridge(this, downstream));
            } else {
                this.pendingConnection = targetServer;

                downstream.setPacketHandler(new SwitchDownstreamHandler(this, targetServer, client));
                downstream.setBatchHandler(new TransferBatchBridge(this, this.upstream));
            }

            downstream.setPacketCodec(this.getProtocol().getCodec());
            downstream.sendPacketImmediately(this.loginPacket);
            downstream.setLogging(true);

            SessionInjections.injectNewDownstream(downstream, this, targetServer);
            this.getLogger().info("[" + this.upstream.getAddress() + "|" + this.getName() + "] -> Downstream [" + targetServer.getServerName() + "] has connected");
        }));
    }

    public void disconnect() {
        this.disconnect(null, false);
    }

    public void disconnect(String reason) {
        this.disconnect(reason, false);
    }

    public void disconnect(String reason, boolean force) {
        PlayerDisconnectEvent event = new PlayerDisconnectEvent(this);
        ProxyServer.getInstance().getEventManager().callEvent(event);


        if (this.upstream != null && !this.upstream.isClosed()) {
            this.upstream.disconnect(reason);
        }

        if (this.serverConnection != null) {
            this.serverConnection.getInfo().removePlayer(this);
            this.serverConnection.disconnect();
        }

        this.proxy.getPlayerManager().removePlayer(this);
        this.getLogger().info("[" + this.getName() + "] -> Upstream has disconnected");
        if (reason != null) this.getLogger().info("[" + this.getName() + "] -> Disconnected with: §c" + reason);
    }

    public void sendPacket(BedrockPacket packet) {
        if (this.upstream != null && !this.upstream.isClosed()) {
            this.upstream.sendPacket(packet);
        }
    }

    @Override
    public void sendMessage(TextContainer message) {
        if (message instanceof TranslationContainer){
            this.sendTranslation((TranslationContainer) message);
        }else {
            this.sendMessage(message.getMessage());
        }
    }

    public void sendTranslation(TranslationContainer textContainer){
        this.sendMessage(this.proxy.translate(textContainer));
    }

    @Override
    public void sendMessage(String message) {
        if (message.trim().isEmpty()){
            return; //Client wont accept empty string
        }

        TextPacket packet = new TextPacket();
        packet.setType(TextPacket.Type.RAW);
        packet.setXuid(this.getXuid());
        packet.setMessage(message);
        this.sendPacket(packet);
    }

    public void sendPopup(String message, String subtitle) {
        TextPacket packet = new TextPacket();
        packet.setType(TextPacket.Type.POPUP);
        packet.setMessage(message);
        packet.setXuid(this.getXuid());
        this.sendPacket(packet);
    }

    public void sendTip(String message) {
        TextPacket packet = new TextPacket();
        packet.setType(TextPacket.Type.TIP);
        packet.setMessage(message);
        packet.setXuid(this.getXuid());
        this.sendPacket(packet);
    }

    public void setSubtitle(String subtitle) {
        SetTitlePacket packet = new SetTitlePacket();
        packet.setType(SetTitlePacket.Type.SUBTITLE);
        packet.setText(subtitle);
        this.sendPacket(packet);
    }

    public void setTitleAnimationTimes(int fadein, int duration, int fadeout) {
        SetTitlePacket packet = new SetTitlePacket();
        packet.setType(SetTitlePacket.Type.TIMES);
        packet.setFadeInTime(fadein);
        packet.setStayTime(duration);
        packet.setFadeOutTime(fadeout);
        packet.setText("");
        this.sendPacket(packet);
    }

    private void setTitle(String text) {
        SetTitlePacket packet = new SetTitlePacket();
        packet.setType(SetTitlePacket.Type.TITLE);
        packet.setText(text);
        this.sendPacket(packet);
    }

    public void clearTitle() {
        SetTitlePacket packet = new SetTitlePacket();
        packet.setType(SetTitlePacket.Type.CLEAR);
        packet.setText("");
        this.sendPacket(packet);
    }

    public void resetTitleSettings() {
        SetTitlePacket packet = new SetTitlePacket();
        packet.setType(SetTitlePacket.Type.RESET);
        packet.setText("");
        this.sendPacket(packet);
    }

    public void sendTitle(String title) {
        this.sendTitle(title, null, 20, 20, 5);
    }

    public void sendTitle(String title, String subtitle) {
        this.sendTitle(title, subtitle, 20, 20, 5);
    }

    public void sendTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        this.setTitleAnimationTimes(fadeIn, stay, fadeOut);
        if (!Strings.isNullOrEmpty(subtitle)) {
            this.setSubtitle(subtitle);
        }
        this.setTitle(Strings.isNullOrEmpty(title) ? " " : title);
    }

    /**
     * Transfer player to another server using "slow" reconnect method
     * @param serverInfo destination server
     */
    public void redirectServer(ServerInfo serverInfo){
        Preconditions.checkNotNull(serverInfo, "Server info can not be null!");
        TransferPacket packet = new TransferPacket();
        packet.setAddress(serverInfo.getPublicAddress().getAddress().getHostAddress());;
        packet.setPort(serverInfo.getPublicAddress().getPort());
        this.sendPacket(packet);
    }

    public boolean addPermission(String permission){
        return this.addPermission(new Permission(permission, true));
    }

    /**
     * Add permission to player
     * @return if the update was successful
     */
    public boolean addPermission(Permission permission){
        Permission oldPerm = this.permissions.get(permission.getName());
        if (oldPerm == null){
            this.permissions.put(permission.getName(), permission);
            return true;
        }
        return oldPerm.getAtomicValue().getAndSet(permission.getValue()) != permission.getValue();
    }

    @Override
    public boolean hasPermission(String permission) {
        if (this.admin){
            return true;
        }
        Permission perm = this.permissions.get(permission.toLowerCase());
        return perm != null && perm.getValue();
    }

    /**
     * Remove permission from player
     * @return if player had this permission
     */
    public boolean removePermission(String permission){
        return this.permissions.remove(permission.toLowerCase()) != null;
    }

    public Permission getPermission(String permission){
        return this.permissions.get(permission.toLowerCase());
    }

    public void setIsAdmin(boolean admin) {
        this.admin = admin;
    }

    public boolean isAdmin() {
        return this.admin;
    }

    @Override
    public boolean isPlayer() {
        return true;
    }

    @Override
    public ProxyServer getProxy() {
        return this.proxy;
    }

    public MainLogger getLogger() {
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

    @Override
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

    public LongSet getBossbars() {
        return this.bossbars;
    }

    public Collection<UUID> getPlayers() {
        return this.players;
    }

    public ObjectSet<String> getScoreboards() {
        return this.scoreboards;
    }

    public void setPluginUpstreamHandler(PacketHandler pluginUpstreamHandler) {
        this.pluginUpstreamHandler = pluginUpstreamHandler;
    }

    public PacketHandler getPluginUpstreamHandler() {
        return this.pluginUpstreamHandler;
    }

    public void setPluginDownstreamHandler(PacketHandler pluginDownstreamHandler) {
        this.pluginDownstreamHandler = pluginDownstreamHandler;
    }

    public PacketHandler getPluginDownstreamHandler() {
        return this.pluginDownstreamHandler;
    }

    public void setAcceptPlayStatus(boolean acceptPlayStatus) {
        this.acceptPlayStatus = acceptPlayStatus;
    }

    public boolean acceptPlayStatus() {
        return this.acceptPlayStatus;
    }
}
