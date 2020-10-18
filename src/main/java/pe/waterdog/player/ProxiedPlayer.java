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
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import lombok.NonNull;
import pe.waterdog.ProxyServer;
import pe.waterdog.command.CommandSender;
import pe.waterdog.event.defaults.PlayerDisconnectEvent;
import pe.waterdog.event.defaults.PlayerLoginEvent;
import pe.waterdog.event.defaults.PreTransferEvent;
import pe.waterdog.logger.MainLogger;
import pe.waterdog.network.ServerInfo;
import pe.waterdog.network.bridge.DownstreamBridge;
import pe.waterdog.network.bridge.TransferBatchBridge;
import pe.waterdog.network.bridge.UpstreamBridge;
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
import pe.waterdog.utils.types.PacketHandler;
import pe.waterdog.utils.types.Permission;
import pe.waterdog.utils.types.TextContainer;
import pe.waterdog.utils.types.TranslationContainer;

import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Base Player class.
 * Base Management of the Player System is done in here.
 */
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
    /**
     * Some downstream server softwares require strict packet sending policy (like PMMP4).
     * To pass packet handler dedicated to SetLocalPlayerAsInitializedPacket only, proxy has to post-complete server transfer.
     * Using this bool allows tells us if we except post-complete phase operation.
     * See ConnectedDownstreamHandler and SwitchDownstreamHandler for exact usage.
     */
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

    /**
     * Called only on the initial connect.
     * Determines the first player the player gets transferred to based on the currently present JoinHandler.
     */
    public void initialConnect() {
        PlayerLoginEvent event = new PlayerLoginEvent(this);
        this.proxy.getEventManager().callEvent(event).whenComplete((futureEvent, ignored) -> {
            if (event.isCancelled()) {
                this.disconnect(event.getCancelReason());
                return;
            }
            SessionInjections.injectUpstreamHandlers(this.upstream, this);

            final ServerInfo forcedHost = this.proxy.getForcedHost(this.loginData.getJoinHostname());
            if (forcedHost != null) {
                this.connect(forcedHost);
                return;
            }
            final ServerInfo initialServer = this.proxy.getJoinHandler().determineServer(this);
            if (initialServer != null) {
                this.connect(initialServer);
            } else {
                this.disconnect(new TranslationContainer("waterdog.no.initial.server"));
            }
        });
    }

    /**
     * Transfers the player to another downstream server
     *
     * @param serverInfo ServerInfo of the target downstream server, can be received using ProxyServer#getServer
     */
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

    /**
     * Disconnects the player, showing no reason
     */
    public void disconnect() {
        this.disconnect((String) null);
    }

    public void disconnect(TextContainer message) {
        if (message instanceof TranslationContainer) {
            this.disconnect(((TranslationContainer) message).getTranslated());
        } else {
            this.disconnect(message.getMessage());
        }
    }

    /**
     * Calls the PlayerDisconnectEvent and disconnects the player from downstream.
     * Kicks the player with the provided reason and closes the connection
     *
     * @param reason The disconnect reason the player will see on his disconnect screen (Supports Color Codes)
     */
    public void disconnect(String reason) {
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

    /**
     * Sends a packet to the upstream connection (client)
     *
     * @param packet the packet to send
     */
    public void sendPacket(BedrockPacket packet) {
        if (this.upstream != null && !this.upstream.isClosed()) {
            this.upstream.sendPacket(packet);
        }
    }

    /**
     * Sends a TextContainer as a message to a player
     *
     * @param message the text container to send, will be translated if instanceof TranslationContainer
     */
    @Override
    public void sendMessage(TextContainer message) {
        if (message instanceof TranslationContainer) {
            this.sendTranslation((TranslationContainer) message);
        } else {
            this.sendMessage(message.getMessage());
        }
    }


    /**
     * Submethod for sending a TranslationContainer to the player's chat window, translates the container and sends the result as a string
     *
     * @param textContainer the TranslationContainer to translate
     */
    public void sendTranslation(TranslationContainer textContainer) {
        this.sendMessage(this.proxy.translate(textContainer));
    }

    /**
     * Sends a message to the player, which will be displayed in the chat window
     *
     * @param message
     */
    @Override
    public void sendMessage(String message) {
        if (message.trim().isEmpty()) {
            return; //Client wont accept empty string
        }

        TextPacket packet = new TextPacket();
        packet.setType(TextPacket.Type.RAW);
        packet.setXuid(this.getXuid());
        packet.setMessage(message);
        this.sendPacket(packet);
    }

    /**
     * Sends a popup to the player
     *
     * @param message  the popup message
     * @param subtitle the subtitle, which will be displayed below the popup
     */
    public void sendPopup(String message, String subtitle) {
        TextPacket packet = new TextPacket();
        packet.setType(TextPacket.Type.POPUP);
        packet.setMessage(message);
        packet.setXuid(this.getXuid());
        this.sendPacket(packet);
    }

    /**
     * Sends a tip message to the player
     *
     * @param message the tip message to send
     */
    public void sendTip(String message) {
        TextPacket packet = new TextPacket();
        packet.setType(TextPacket.Type.TIP);
        packet.setMessage(message);
        packet.setXuid(this.getXuid());
        this.sendPacket(packet);
    }

    /**
     * Sends a subtitle in addition to a title
     *
     * @param subtitle the subtitle to send as a string
     */
    public void setSubtitle(String subtitle) {
        SetTitlePacket packet = new SetTitlePacket();
        packet.setType(SetTitlePacket.Type.SUBTITLE);
        packet.setText(subtitle);
        this.sendPacket(packet);
    }

    /**
     * Sets the animation time of a title
     *
     * @param fadein   the fade-in time of the title
     * @param duration the display duration of the title
     * @param fadeout  the fade-out time of the title
     */
    public void setTitleAnimationTimes(int fadein, int duration, int fadeout) {
        SetTitlePacket packet = new SetTitlePacket();
        packet.setType(SetTitlePacket.Type.TIMES);
        packet.setFadeInTime(fadein);
        packet.setStayTime(duration);
        packet.setFadeOutTime(fadeout);
        packet.setText("");
        this.sendPacket(packet);
    }

    /**
     * Sets the current displayed title
     *
     * @param text the text to send
     */
    private void setTitle(String text) {
        SetTitlePacket packet = new SetTitlePacket();
        packet.setType(SetTitlePacket.Type.TITLE);
        packet.setText(text);
        this.sendPacket(packet);
    }

    /**
     * Clears the title of the player
     */
    public void clearTitle() {
        SetTitlePacket packet = new SetTitlePacket();
        packet.setType(SetTitlePacket.Type.CLEAR);
        packet.setText("");
        this.sendPacket(packet);
    }

    /**
     * Resets all currently applied title settings
     */
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

    /**
     * Sends a title with the provided animation details and the given subtitle to the player
     *
     * @param title    the main title text
     * @param subtitle the subtitle displayed below
     * @param fadeIn   the time it takes the title to fade in
     * @param stay     the time it takes until fadeOut-time is starting
     * @param fadeOut  the time it takes until the title disappeared
     */
    public void sendTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        this.setTitleAnimationTimes(fadeIn, stay, fadeOut);
        if (!Strings.isNullOrEmpty(subtitle)) {
            this.setSubtitle(subtitle);
        }
        this.setTitle(Strings.isNullOrEmpty(title) ? " " : title);
    }

    /**
     * Transfer player to another server using "slow" reconnect method
     *
     * @param serverInfo destination server
     */
    public void redirectServer(ServerInfo serverInfo) {
        Preconditions.checkNotNull(serverInfo, "Server info can not be null!");
        TransferPacket packet = new TransferPacket();
        packet.setAddress(serverInfo.getPublicAddress().getAddress().getHostAddress());
        packet.setPort(serverInfo.getPublicAddress().getPort());
        this.sendPacket(packet);
    }

    /**
     * Adds a permission to the player
     *
     * @param permission the permission to give him
     * @return whether the update was successful or not
     */
    public boolean addPermission(String permission) {
        return this.addPermission(new Permission(permission, true));
    }

    /**
     * Add permission to the player
     *
     * @return if the update was successful
     */
    public boolean addPermission(Permission permission) {
        Permission oldPerm = this.permissions.get(permission.getName());
        if (oldPerm == null) {
            this.permissions.put(permission.getName(), permission);
            return true;
        }
        return oldPerm.getAtomicValue().getAndSet(permission.getValue()) != permission.getValue();
    }

    /**
     * @param permission the permission to check for
     * @return Returns whether the player has the passed permission or not
     */
    @Override
    public boolean hasPermission(String permission) {
        if (this.admin) {
            return true;
        }
        Permission perm = this.permissions.get(permission.toLowerCase());
        return perm != null && perm.getValue();
    }

    /**
     * Remove permission from player
     *
     * @return if player had this permission
     */
    public boolean removePermission(String permission) {
        return this.permissions.remove(permission.toLowerCase()) != null;
    }

    /**
     * @param permission the permission name to search for
     * @return the Instance of Permission if present
     */
    public Permission getPermission(String permission) {
        return this.permissions.get(permission.toLowerCase());
    }

    /**
     * @return true if the player has administrator status, false if not
     */
    public boolean isAdmin() {
        return this.admin;
    }

    /**
     * Sets whether this player should have Administrator Status.
     * Players with administrator status are granted every permissions, even if not specificly applied
     *
     * @param admin Whether the player is admin or not
     */
    public void setAdmin(boolean admin) {
        this.admin = admin;
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
