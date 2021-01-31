/*
 * Copyright 2021 WaterdogTEAM
 * Licensed under the GNU General Public License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.waterdog.player;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.nukkitx.protocol.bedrock.*;
import com.nukkitx.protocol.bedrock.packet.*;
import dev.waterdog.ProxyServer;
import dev.waterdog.command.CommandSender;
import dev.waterdog.event.defaults.*;
import dev.waterdog.logger.MainLogger;
import dev.waterdog.network.ServerInfo;
import dev.waterdog.network.bridge.DownstreamBridge;
import dev.waterdog.network.bridge.TransferBatchBridge;
import dev.waterdog.network.bridge.UpstreamBridge;
import dev.waterdog.network.downstream.InitialHandler;
import dev.waterdog.network.downstream.SwitchDownstreamHandler;
import dev.waterdog.network.protocol.ProtocolVersion;
import dev.waterdog.network.rewrite.RewriteMaps;
import dev.waterdog.network.rewrite.types.RewriteData;
import dev.waterdog.network.session.LoginData;
import dev.waterdog.network.session.ServerConnection;
import dev.waterdog.network.session.SessionInjections;
import dev.waterdog.utils.types.PacketHandler;
import dev.waterdog.utils.types.Permission;
import dev.waterdog.utils.types.TextContainer;
import dev.waterdog.utils.types.TranslationContainer;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Base Player class.
 * Base Management of the Player System is done in here.
 */
public class ProxiedPlayer implements CommandSender {

    private final ProxyServer proxy;

    private final BedrockServerSession upstream;
    private final AtomicBoolean disconnected = new AtomicBoolean(false);
    private final RewriteData rewriteData = new RewriteData();
    private final LoginData loginData;
    private final RewriteMaps rewriteMaps;
    private final LongSet entities = new LongOpenHashSet();
    private final LongSet bossbars = new LongOpenHashSet();
    private final Collection<UUID> players = new HashSet<>();
    private final ObjectSet<String> scoreboards = new ObjectOpenHashSet<>();
    private final Object2ObjectMap<String, Permission> permissions = new Object2ObjectOpenHashMap<>();
    private ServerConnection serverConnection;
    private ServerInfo pendingConnection;
    private boolean admin = false;
    /**
     * Signalizes if connection bridges can do entity and block rewrite.
     * Since first StarGamePacket was received we start with entity id and block rewrite.
     */
    private volatile boolean canRewrite = false;
    private volatile boolean hasUpstreamBridge = false;
    /**
     * Some downstream server software require strict packet sending policy (like PMMP4).
     * To pass packet handler dedicated to SetLocalPlayerAsInitializedPacket only, proxy has to post-complete server transfer.
     * Using this bool allows tells us if we except post-complete phase operation.
     * See ConnectedDownstreamHandler and SwitchDownstreamHandler for exact usage.
     */
    private volatile boolean acceptPlayStatus = false;
    /**
     * Used to determine if proxy can send resource packs packets to player.
     * This value is changed by PlayerResourcePackInfoSendEvent.
     */
    private volatile boolean acceptResourcePacks = true;
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
        this.rewriteMaps = new RewriteMaps(this);
        this.proxy.getPlayerManager().subscribePermissions(this);
    }

    /**
     * Called after sending LOGIN_SUCCESS in PlayStatusPacket.
     */
    public void initPlayer() {
        SessionInjections.injectUpstreamHandlers(this.upstream, this);
        if (!this.proxy.getConfiguration().enabledResourcePacks()) {
            this.initialConnect();
            return;
        }

        ResourcePacksInfoPacket packet = this.proxy.getPackManager().getPacksInfoPacket();
        PlayerResourcePackInfoSendEvent event = new PlayerResourcePackInfoSendEvent(this, packet);
        this.proxy.getEventManager().callEvent(event);
        if (event.isCancelled()) {
            // Connect player to downstream without sending ResourcePacksInfoPacket
            this.acceptResourcePacks = false;
            this.initialConnect();
            return;
        }
        this.upstream.sendPacket(event.getPacket());
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

            // Determine forced host first
            ServerInfo initialServer = this.proxy.getForcedHost(this.loginData.getJoinHostname());
            if (initialServer == null) {
                initialServer = this.proxy.getJoinHandler().determineServer(this);
            }

            if (initialServer == null) {
                this.disconnect(new TranslationContainer("waterdog.no.initial.server"));
                return;
            }

            // Event should not change initial server. For we use join handler.
            InitialServerDeterminationEvent serverEvent = new InitialServerDeterminationEvent(this, initialServer);
            this.proxy.getEventManager().callEvent(serverEvent);
            this.connect(initialServer);
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

        ServerInfo targetServer = event.getTargetServer();
        if (this.serverConnection != null && this.serverConnection.getInfo() == targetServer) {
            this.sendMessage(new TranslationContainer("waterdog.downstream.connected", serverInfo.getServerName()));
            return;
        }

        if (this.pendingConnection == targetServer) {
            this.sendMessage(new TranslationContainer("waterdog.downstream.connecting", serverInfo.getServerName()));
            return;
        }

        if (this.serverConnection != null) {
            this.pendingConnection = targetServer;
        }

        CompletableFuture<BedrockClient> future = this.proxy.bindClient(this.getProtocol());
        future.thenAccept(client -> client.connect(targetServer.getAddress()).whenComplete((downstream, error) -> {
            if (this.disconnected.get()) {
                client.close();
                this.getLogger().debug("Discarding downstream connection: Player " + this.getName() +" disconnected!");
                return;
            }

            if (error != null) {
                this.connectFailure(client, targetServer, error);
                return;
            }

            if (this.serverConnection == null) {
                this.serverConnection = new ServerConnection(client, downstream, targetServer);
                targetServer.addPlayer(this);

                downstream.setPacketHandler(new InitialHandler(this));
                downstream.setBatchHandler(new DownstreamBridge(this, this.upstream));
                this.upstream.setBatchHandler(new UpstreamBridge(this, downstream));
                this.hasUpstreamBridge = true;
            } else {
                downstream.setPacketHandler(new SwitchDownstreamHandler(this, targetServer, client));
                downstream.setBatchHandler(new TransferBatchBridge(this, this.upstream));
            }

            downstream.setPacketCodec(this.getProtocol().getCodec());
            downstream.sendPacketImmediately(this.loginData.getLoginPacket());
            downstream.setLogging(true);

            SessionInjections.injectNewDownstream(downstream, this, targetServer, client);
            this.getLogger().info("[" + this.getAddress() + "|" + this.getName() + "] -> Downstream [" + targetServer.getServerName() + "] has connected");
        })).whenComplete((ignore, error) -> {
            if (error != null) {
                this.connectFailure(null, targetServer, error);
            }
        });
    }

    private void connectFailure(BedrockClient client, ServerInfo targetServer, Throwable error) {
        this.getLogger().debug("[" + this.getAddress() + "|" + this.getName() + "] Unable to connect to downstream " + targetServer.getServerName(), error);
        this.pendingConnection = null;
        if (client != null) {
            client.close();
        }

        String exceptionMessage = error.getLocalizedMessage();
        if (this.sendToFallback(targetServer, exceptionMessage)) {
            this.sendMessage(new TranslationContainer("waterdog.connected.fallback", targetServer.getServerName()));
        } else {
            this.disconnect(new TranslationContainer("waterdog.downstream.transfer.failed", targetServer.getServerName(), exceptionMessage));
        }
    }

    /**
     * Disconnects the player, showing no reason
     */
    public void disconnect() {
        this.disconnect((String) null);
    }

    public void disconnect(TextContainer message) {
        this.disconnect(message, false);
    }

    public void disconnect(TextContainer message, boolean forceClose) {
        if (message instanceof TranslationContainer) {
            this.disconnect(((TranslationContainer) message).getTranslated(), forceClose);
        } else {
            this.disconnect(message.getMessage(), forceClose);
        }
    }

    public void disconnect(String reason) {
        this.disconnect(reason, false);
    }

    /**
     * Calls the PlayerDisconnectEvent and disconnects the player from downstream.
     * Kicks the player with the provided reason and closes the connection
     *
     * @param reason The disconnect reason the player will see on his disconnect screen (Supports Color Codes)
     * @param forceClose whatever force close connections
     */
    public void disconnect(String reason, boolean forceClose) {
        if (!this.disconnected.compareAndSet(false, true)) {
            return;
        }

        PlayerDisconnectEvent event = new PlayerDisconnectEvent(this);
        this.proxy.getEventManager().callEvent(event);

        if (this.upstream != null && !this.upstream.isClosed()) {
            this.upstream.disconnect(reason);
        }

        if (this.serverConnection != null) {
            this.serverConnection.getInfo().removePlayer(this);
            this.serverConnection.disconnect(forceClose);
        }

        this.proxy.getPlayerManager().removePlayer(this);
        this.getLogger().info("[" + this.getAddress() + "|" + this.getName() + "] -> Upstream has disconnected");
        if (reason != null) this.getLogger().info("[" + this.getName() + "] -> Disconnected with: §c" + reason);
    }

    /**
     * Send player to fallback server if any exists.
     *
     * @param oldServer server from which was player disconnected.
     * @param reason disconnected reason.
     * @return if connection to downstream was successful.
     */
    public boolean sendToFallback(ServerInfo oldServer, String reason) {
        ServerInfo fallbackServer = this.proxy.getReconnectHandler().getFallbackServer(this, oldServer, reason);
        if (fallbackServer != null && fallbackServer != this.serverConnection.getInfo()) {
            this.connect(fallbackServer);
            return true;
        }
        return false;
    }

    public void onDownstreamTimeout() {
        ServerInfo serverInfo = this.serverConnection.getInfo();
        if (!this.sendToFallback(serverInfo, "Downstream Timeout")) {
            this.disconnect(new TranslationContainer("waterdog.downstream.down", serverInfo.getServerName(), "Timeout"));
        }
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
            return; // Client wont accept empty string
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
        if (this.admin || permission.isEmpty() || permission.equals("*")) {
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

    public long getPing() {
        return this.upstream.getLatency();
    }

    /**
     * Safe way to get player's ServerInfo and to prevent NullPointer exception
     * Server connection may be null when player is connecting first time.
     *
     * @return ServerInfo if player is connected to downstream
     */
    public ServerInfo getServerInfo() {
        return this.serverConnection == null ? null : this.serverConnection.getInfo();
    }

    public InetSocketAddress getAddress() {
        return this.upstream == null? null : this.upstream.getAddress();
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

    public boolean isConnected() {
        return !this.disconnected.get() && this.upstream != null && !this.upstream.isClosed();
    }

    public RewriteMaps getRewriteMaps() {
        return this.rewriteMaps;
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

    public ProtocolVersion getProtocol() {
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

    public boolean hasUpstreamBridge() {
        return this.hasUpstreamBridge;
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

    public PacketHandler getPluginUpstreamHandler() {
        return this.pluginUpstreamHandler;
    }

    public void setPluginUpstreamHandler(PacketHandler pluginUpstreamHandler) {
        this.pluginUpstreamHandler = pluginUpstreamHandler;
    }

    public PacketHandler getPluginDownstreamHandler() {
        return this.pluginDownstreamHandler;
    }

    public void setPluginDownstreamHandler(PacketHandler pluginDownstreamHandler) {
        this.pluginDownstreamHandler = pluginDownstreamHandler;
    }

    public void setAcceptPlayStatus(boolean acceptPlayStatus) {
        this.acceptPlayStatus = acceptPlayStatus;
    }

    public boolean acceptPlayStatus() {
        return this.acceptPlayStatus;
    }

    public boolean acceptResourcePacks() {
        return this.acceptResourcePacks;
    }

    @Override
    public String toString() {
        return "ProxiedPlayer(displayName=" + this.getName() +
                ", protocol=" + this.getProtocol() +
                ", connected=" + this.isConnected() +
                ", address=" + this.getAddress() +
                ", serverInfo=" + this.getServerInfo() +
                ")";
    }
}
