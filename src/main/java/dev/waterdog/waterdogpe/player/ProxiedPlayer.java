/*
 * Copyright 2022 WaterdogTEAM
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

package dev.waterdog.waterdogpe.player;

import dev.waterdog.waterdogpe.network.connection.codec.compression.CompressionType;
import dev.waterdog.waterdogpe.network.connection.handler.IReconnectHandler;
import dev.waterdog.waterdogpe.network.connection.handler.ReconnectReason;
import dev.waterdog.waterdogpe.network.connection.peer.BedrockServerSession;
import dev.waterdog.waterdogpe.network.connection.client.ClientConnection;
import dev.waterdog.waterdogpe.network.protocol.handler.PluginPacketHandler;
import dev.waterdog.waterdogpe.network.protocol.handler.TransferCallback;
import dev.waterdog.waterdogpe.network.protocol.handler.downstream.CompressionInitHandler;
import dev.waterdog.waterdogpe.network.protocol.user.LoginData;
import dev.waterdog.waterdogpe.network.protocol.user.Platform;
import dev.waterdog.waterdogpe.network.protocol.handler.downstream.InitialHandler;
import dev.waterdog.waterdogpe.network.protocol.handler.downstream.SwitchDownstreamHandler;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.protocol.bedrock.data.HudElement;
import org.cloudburstmc.protocol.bedrock.data.ScoreInfo;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType;
import org.cloudburstmc.protocol.bedrock.data.command.CommandOriginData;
import org.cloudburstmc.protocol.bedrock.data.command.CommandOriginType;
import org.cloudburstmc.protocol.bedrock.packet.*;
import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.command.CommandSender;
import dev.waterdog.waterdogpe.event.defaults.*;
import dev.waterdog.waterdogpe.logger.MainLogger;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo;
import dev.waterdog.waterdogpe.network.protocol.ProtocolVersion;
import dev.waterdog.waterdogpe.network.protocol.rewrite.RewriteMaps;
import dev.waterdog.waterdogpe.network.protocol.rewrite.types.RewriteData;
import dev.waterdog.waterdogpe.network.protocol.handler.upstream.ResourcePacksHandler;
import dev.waterdog.waterdogpe.network.protocol.handler.upstream.ConnectedUpstreamHandler;
import dev.waterdog.waterdogpe.utils.types.Permission;
import dev.waterdog.waterdogpe.utils.types.TextContainer;
import dev.waterdog.waterdogpe.utils.types.TranslationContainer;
import io.netty.util.concurrent.Future;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.objects.*;
import org.cloudburstmc.protocol.common.util.Preconditions;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Base Player class.
 * Base Management of the Player System is done in here.
 */
public class ProxiedPlayer implements CommandSender {
    private final ProxyServer proxy;

    @Getter
    private final BedrockServerSession connection;
    @Getter
    private final CompressionType compression;

    private final AtomicBoolean disconnected = new AtomicBoolean(false);
    private final AtomicBoolean loginCalled = new AtomicBoolean(false);
    private final AtomicBoolean loginCompleted = new AtomicBoolean(false);
    private volatile CharSequence disconnectReason;

    /**
     * Hard cap on how long the async {@link PlayerLoginEvent} future may take to complete.
     * If a plugin handler registers a CompletableFuture that never completes (e.g. a hung
     * Redis/coroutine call), the player would otherwise be stranded forever: loginCompleted
     * never flips, so {@link #disconnect(CharSequence)} can never evict it. The timeout
     * guarantees the future always settles, which always releases the player.
     */
    private static final long LOGIN_EVENT_TIMEOUT_SECONDS = 60;

    /**
     * Hard cap on how long the async {@link ServerPreConnectEvent} future may take before we dial the
     * target anyway. Mirrors {@link #LOGIN_EVENT_TIMEOUT_SECONDS}: a hung handler future must never leave
     * the player without a connection or {@code targetServer} stuck in {@link #pendingServers} forever.
     */
    private static final long PRE_CONNECT_EVENT_TIMEOUT_SECONDS = 60;

    /**
     * Hard cap on how long an established downstream connection may take until StartGamePacket
     * claims the transfer. A downstream that keeps the connection alive without ever completing
     * the login would otherwise leave the pending connection stuck forever. This failure is
     * still recoverable, so it goes through {@link #onTransferFailure} instead of a kick.
     */
    private static final int PENDING_CONNECTION_TIMEOUT_SECONDS = 60;

    @Getter
    private final RewriteData rewriteData = new RewriteData();
    @Getter
    private final LoginData loginData;
    @Getter
    private final RewriteMaps rewriteMaps;
    @Getter
    private final LongSet entities = LongSets.synchronize(new LongOpenHashSet());
    @Getter
    private final LongSet bossbars = LongSets.synchronize(new LongOpenHashSet());
    private final ObjectSet<UUID> players = ObjectSets.synchronize(new ObjectOpenHashSet<>());
    @Getter
    private final ObjectSet<String> scoreboards = ObjectSets.synchronize(new ObjectOpenHashSet<>());
    @Getter
    private final Long2ObjectMap<ScoreInfo> scoreInfos = Long2ObjectMaps.synchronize(new Long2ObjectOpenHashMap<>());
    @Getter
    private final Long2LongMap entityLinks = Long2LongMaps.synchronize(new Long2LongOpenHashMap());
    @Getter
    private final LongSet chunkBlobs = LongSets.synchronize(new LongOpenHashSet());
    @Getter
    private final Int2IntMap volumeEntities = Int2IntMaps.synchronize(new Int2IntOpenHashMap()); // id -> dimension
    @Getter
    private final Set<HudElement> hiddenHudElements = ObjectSets.synchronize(new ObjectOpenHashSet<>());
    @Getter @Setter
    private volatile boolean fogApplied;
    @Getter @Setter
    private volatile int inputLockData;
    @Getter
    private final Int2ObjectMap<ContainerType> openContainers = Int2ObjectMaps.synchronize(new Int2ObjectOpenHashMap<>()); // id -> type
    private final Object2ObjectMap<String, Permission> permissions = new Object2ObjectOpenHashMap<>();
    private final Collection<ServerInfo> pendingServers = ObjectCollections.synchronize(new ObjectArrayList<>());
    private ClientConnection clientConnection;
    private ClientConnection pendingConnection;


    /**
     *  Whether this player should have administrator status.
     *  Players with administrator status are granted every permission, even if not specifically applied.
     */
    @Setter
    @Getter
    private boolean admin = false;
    /**
     * Signalizes if connection bridges can do entity and block rewrite.
     * Since first StartGamePacket was received, we start with entity id and block rewrite.
     */
    @Setter
    private volatile boolean canRewrite = false;
    private volatile boolean hasUpstreamBridge = false;
    /**
     * Used to determine if proxy can send resource packs packets to player.
     * This value is changed by PlayerResourcePackInfoSendEvent.
     */
    private volatile boolean acceptResourcePacks = true;
    /**
     * Used to determine if proxy can send ItemComponentPacket to player.
     * Client will crash if ItemComponentPacket is sent twice.
     */
    @Setter
    private volatile boolean acceptItemComponentPacket = true;
    /**
     * Whether the current downstream server serves chunks using the sub-chunk request system
     * (LevelChunkPacket with a negative sub-chunk count). When set, injected empty chunks must use
     * request mode too, otherwise the client breaks instead of requesting the sub-chunks.
     */
    @Getter
    @Setter
    private volatile boolean subChunkRequestMode = false;
    /**
     * Additional downstream and upstream handlers can be set by plugin.
     * Do not set directly BedrockPacketHandler to sessions!
     */
    @Getter
    private final Collection<PluginPacketHandler> pluginPacketHandlers = new ObjectArrayList<>();

    public ProxiedPlayer(ProxyServer proxy, BedrockServerSession session, CompressionType compression, LoginData loginData) {
        this.proxy = proxy;
        this.connection = session;
        this.compression = compression;
        this.loginData = loginData;
        this.rewriteMaps = new RewriteMaps(this);
        this.proxy.getPlayerManager().subscribePermissions(this);
        this.connection.addDisconnectListener(this::disconnect);
        this.rewriteData.setCodecHelper(session.getPeer().getCodecHelper());
    }

    /**
     * Called after sending LOGIN_SUCCESS in PlayStatusPacket.
     */
    public void initPlayer() {
        if (!this.loginCalled.compareAndSet(false, true)) {
            return;
        }

        PlayerLoginEvent event = new PlayerLoginEvent(this);
        this.proxy.getEventManager().callEvent(event)
                // Never let a misbehaving (hung) async login handler strand the player forever.
                .orTimeout(LOGIN_EVENT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .whenComplete((futureEvent, error) -> {
            this.loginCompleted.set(true);

            if (error != null) {
                if (error instanceof TimeoutException) {
                    this.getLogger().warning("[{}|{}] PlayerLoginEvent did not complete within {}s, forcing disconnect",
                            this.getAddress(), this.getName(), LOGIN_EVENT_TIMEOUT_SECONDS);
                } else {
                    this.getLogger().throwing(error);
                }
                this.disconnect(new TranslationContainer("waterdog.downstream.initial.connect"));
                return;
            }

            if (event.isCancelled()) {
                this.disconnect(event.getCancelReason());
                return;
            }

            if (!this.isConnected() || this.disconnectReason != null) { // player might have disconnected itself
                this.disconnect(this.disconnectReason == null ? "Already disconnected" : this.disconnectReason);
                return;
            }

            if (this.proxy.getConfiguration().enableResourcePacks()) {
                this.sendResourcePacks();
            } else {
                this.initialConnect();
            }
        });
    }

    private void sendResourcePacks() {
        ResourcePacksInfoPacket packet = this.proxy.getPackManager().getPacksInfoPacket(this.getDevicePlatform());
        PlayerResourcePackInfoSendEvent event = new PlayerResourcePackInfoSendEvent(this, packet);
        this.proxy.getEventManager().callEvent(event);
        if (event.isCancelled()) {
            // Connect player to downstream without sending ResourcePacksInfoPacket
            this.acceptResourcePacks = false;
            this.initialConnect();
        } else {
            this.connection.setPacketHandler(new ResourcePacksHandler(this));
            this.connection.sendPacket(event.getPacket());
        }
    }

    /**
     * Called only on the initial connect.
     * Determines the first player the player gets transferred to based on the currently present JoinHandler.
     */
    public final void initialConnect() {
        if (this.disconnected.get()) {
            return;
        }

        this.connection.setPacketHandler(new ConnectedUpstreamHandler(this));
        // Determine forced host first
        ServerInfo initialServer = this.proxy.getForcedHostHandler().resolveForcedHost(this.loginData.getJoinHostname(), this);
        if (initialServer == null) {
            initialServer = this.proxy.getJoinHandler().determineServer(this);
        }

        if (initialServer == null) {
            this.disconnect(new TranslationContainer("waterdog.no.initial.server"));
            return;
        }

        // Event should not change initial server. For we use join handler.
        InitialServerDeterminedEvent serverEvent = new InitialServerDeterminedEvent(this, initialServer);
        this.proxy.getEventManager().callEvent(serverEvent);
        this.connect(initialServer);
    }

    /**
     * Transfers the player to another downstream server
     *
     * @param serverInfo ServerInfo of the target downstream server, can be received using ProxyServer#getServer
     */
    public void connect(ServerInfo serverInfo) {
        Preconditions.checkNotNull(serverInfo, "Server info can not be null!");
        Preconditions.checkArgument(this.isConnected(), "User not connected");
        Preconditions.checkArgument(this.loginCompleted.get(), "User not logged in");

        ServerTransferRequestEvent event = new ServerTransferRequestEvent(this, serverInfo);
        ProxyServer.getInstance().getEventManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }

        ServerInfo targetServer = event.getTargetServer();
        if (this.clientConnection != null && this.clientConnection.getServerInfo() == targetServer) {
            this.sendMessage(new TranslationContainer("waterdog.downstream.connected", targetServer.getServerName()));
            return;
        }

        if (this.pendingServers.contains(targetServer)) {
            this.sendMessage(new TranslationContainer("waterdog.downstream.connecting", targetServer.getServerName()));
            return;
        }

        // Deny new transfers while another one is mid-flight: starting a second transfer before the
        // dimension change sequence completes corrupts the client state and the transfer queue.
        TransferCallback activeTransfer = this.rewriteData.getTransferCallback();
        if (activeTransfer != null && activeTransfer.getPhase() != TransferCallback.TransferPhase.RESET) {
            this.sendMessage(new TranslationContainer("waterdog.downstream.connecting", activeTransfer.getTargetServer().getServerName()));
            this.getLogger().debug("[{}] Denied transfer to {}: transfer to {} is still in progress",
                    this.getName(), targetServer.getServerName(), activeTransfer.getTargetServer().getServerName());
            return;
        }

        this.pendingServers.add(targetServer);

        ClientConnection connectingServer = this.getPendingConnection();
        if (connectingServer != null) {
            if (connectingServer.getServerInfo() == targetServer) {
                this.pendingServers.remove(targetServer);
                this.sendMessage(new TranslationContainer("waterdog.downstream.connecting", targetServer.getServerName()));
                return;
            } else {
                // Clear the pending slot first so the close event reads as a deliberate discard,
                // not as a recoverable transfer failure.
                this.setPendingConnection(null);
                connectingServer.disconnect();
                this.getLogger().debug("Discarding pending connection for " + this.getName() + "! Tried to join " + targetServer.getServerName());
            }
        }

        // Give plugins a chance to hold out the connection (e.g. to save the player's inventory or other
        // state on the current server) before we dial the target. The event is async and completable:
        // any future a handler registers delays createConnection until it settles.
        ServerPreConnectEvent preConnectEvent = new ServerPreConnectEvent(this, this.getServerInfo(), targetServer);
        this.proxy.getEventManager().callEvent(preConnectEvent)
                // Never let a hung handler future leave the player without a connection or targetServer pending forever.
                .orTimeout(PRE_CONNECT_EVENT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .whenComplete((ev, error) -> {
                    if (error != null) {
                        // A hung or failing hold-out leaves the player's state ambiguous (e.g. a half-saved
                        // inventory), so abort the transfer and disconnect rather than dial the target anyway.
                        if (error instanceof TimeoutException) {
                            this.getLogger().warning("[{}|{}] ServerPreConnectEvent did not complete within {}s, disconnecting",
                                    this.getAddress(), this.getName(), PRE_CONNECT_EVENT_TIMEOUT_SECONDS);
                        } else {
                            this.getLogger().throwing(error);
                        }
                        this.pendingServers.remove(targetServer);
                        this.disconnect(new TranslationContainer("waterdog.downstream.transfer.failed",
                                targetServer.getServerName(),
                                error instanceof TimeoutException ? "timed out" : error.getClass().getSimpleName()));
                        return;
                    }
                    if (ev.isCancelled()) {
                        this.pendingServers.remove(targetServer);
                        return;
                    }

                    if (!this.isConnected()) { // player might have disconnected while the event was processed
                        this.pendingServers.remove(targetServer);
                        return;
                    }

                    this.createConnection(targetServer);
                });
    }

    private void createConnection(ServerInfo targetServer) {
        Future<ClientConnection> connectionFuture;
        try {
            // A custom ServerInfo implementation may throw instead of failing the future; without
            // this guard that would leak the pendingServers entry and block the target forever.
            connectionFuture = Objects.requireNonNull(targetServer.createConnection(this), "createConnection returned null");
        } catch (Throwable error) {
            this.pendingServers.remove(targetServer);
            this.connectFailure(null, targetServer, error);
            return;
        }

        connectionFuture.addListener(future -> {
            ClientConnection connection = null;
            try {
                if (future.cause() == null) {
                    this.connect0(targetServer, connection = (ClientConnection) future.get());
                } else {
                    this.connectFailure(null, targetServer, future.cause());
                }
            } catch (Throwable e) {
                this.connectFailure(connection, targetServer, e);
                this.clearPendingConnection(connection);
            } finally {
                this.pendingServers.remove(targetServer);
            }
        });
    }

    private void connect0(ServerInfo targetServer, ClientConnection connection) {
        if (!this.isConnected()) {
            connection.disconnect();
            return;
        }

        ServerConnectedEvent event = new ServerConnectedEvent(this, connection);
        this.getProxy().getEventManager().callEvent(event);
        if (event.isCancelled() || !connection.isConnected()) {
            if (connection.isConnected()) {
                connection.disconnect();
            }
            return;
        }

        this.setPendingConnection(connection);
        this.proxy.getScheduler().scheduleDelayed(() -> {
            if (this.getPendingConnection() == connection) {
                this.getLogger().warning("[{}|{}] Downstream {} did not send StartGame within {}s",
                        this.getAddress(), this.getName(), targetServer.getServerName(), PENDING_CONNECTION_TIMEOUT_SECONDS);
                this.onTransferFailure(connection, targetServer, ReconnectReason.TIMEOUT, "Transfer timed out");
            }
        }, PENDING_CONNECTION_TIMEOUT_SECONDS * 20);

        connection.setCodecHelper(this.getProtocol().getCodec(),
                this.connection.getPeer().getCodecHelper());

        BedrockPacketHandler handler;
        if (this.clientConnection == null) {
            ((ConnectedUpstreamHandler) this.connection.getPacketHandler()).setTargetConnection(connection);
            this.hasUpstreamBridge = true;
            handler = new InitialHandler(this, connection);
        } else {
            handler = new SwitchDownstreamHandler(this, connection);
        }

        if (this.getProtocol().isAfterOrEqual(ProtocolVersion.MINECRAFT_PE_1_19_30)) {
            connection.setPacketHandler(new CompressionInitHandler(this, connection, handler));
        } else {
            connection.setPacketHandler(handler);
            connection.sendPacket(this.loginData.getLoginPacket());
        }

        this.getLogger().info("[{}|{}] -> Downstream [{}] has connected", connection.getSocketAddress(), this.getName(), targetServer.getServerName());
    }

    private void connectFailure(ClientConnection connection, ServerInfo targetServer, Throwable error) {
        if (connection != null) {
            connection.disconnect();
        }

        if (this.disconnected.get()) {
            return;
        }

        this.getLogger().error("[{}|{}] Unable to connect to downstream {}", this.getAddress(), this.getName(), targetServer.getServerName(), error);
        String exceptionMessage = Objects.requireNonNullElse(error.getLocalizedMessage(), error.getClass().getSimpleName());
        this.onTransferFailure(connection, targetServer, ReconnectReason.EXCEPTION, exceptionMessage);
    }

    /**
     * Called when a connection to a new downstream server fails before StartGamePacket was received.
     * The player is still fully connected to the previous downstream at this point, so the failure is
     * recoverable: {@link IReconnectHandler#getTransferFailureServer} decides whether the player
     * reconnects to another server or stays on the current one.
     *
     * @param connection   the failed connection, or null if it was never established
     * @param targetServer the server that could not be reached
     */
    public final void onTransferFailure(ClientConnection connection, ServerInfo targetServer, ReconnectReason reason, String message) {
        TransferCallback transferCallback = this.rewriteData.getTransferCallback();
        if (connection != null && transferCallback != null && transferCallback.getConnection() == connection) {
            // The connection already claimed the transfer: past the recoverable window,
            // the mid-transfer failure paths own it now.
            return;
        }

        if (connection != null) {
            // Only the current pending attempt can trigger recovery; discarded connections just die.
            if (!this.clearPendingConnection(connection)) {
                connection.disconnect();
                return;
            }
            connection.disconnect();
        }

        if (this.disconnected.get()) {
            return;
        }

        boolean recoverable = this.clientConnection != null && this.clientConnection.isConnected();
        this.proxy.getEventManager().callEvent(new ServerTransferFailedEvent(this, targetServer, reason, message, recoverable));

        if (connection == null && this.getPendingConnection() != null) {
            // A dial failed while a newer connection attempt is already in flight: don't stomp it.
            this.sendMessage(new TranslationContainer("waterdog.downstream.transfer.failed", targetServer.getServerName(), message));
            return;
        }

        if (!recoverable) {
            // No healthy downstream to stay on: regular fallback behavior.
            if (this.sendToFallback(targetServer, reason, message)) {
                this.sendMessage(new TranslationContainer("waterdog.connected.fallback", targetServer.getServerName()));
            } else {
                this.disconnect(new TranslationContainer("waterdog.downstream.transfer.failed", targetServer.getServerName(), message));
            }
            return;
        }

        ServerInfo reconnectServer;
        try {
            reconnectServer = this.proxy.getReconnectHandler().getTransferFailureServer(this, targetServer, reason, message);
        } catch (Throwable t) {
            // A throwing reconnect handler must not abort recovery; treat it as "stay".
            this.getLogger().error("[" + this.getName() + "] ReconnectHandler getTransferFailureServer threw", t);
            reconnectServer = null;
        }
        if (reconnectServer == null || reconnectServer == this.getServerInfo()) {
            // Stay on the previous downstream, which is still fully functional.
            this.sendMessage(new TranslationContainer("waterdog.downstream.transfer.failed", targetServer.getServerName(), message));
            return;
        }

        this.getLogger().debug("[{}] Transfer to {} failed: reconnecting to {}", this.getName(), targetServer.getServerName(), reconnectServer.getServerName());
        this.connect(reconnectServer);
    }

    /**
     * Disconnects the player, showing no reason
     */
    public void disconnect() {
        this.disconnect((String) null);
    }

    public void disconnect(TextContainer message) {
        if (message instanceof TranslationContainer tr) {
            this.disconnect(tr.getTranslated());
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
    public void disconnect(CharSequence reason) {
        if (this.loginCalled.get() && !this.loginCompleted.get()) {
            // Wait until PlayerLoginEvent completes
            this.disconnectReason = reason;
            return;
        }

        if (!this.disconnected.compareAndSet(false, true)) {
            return;
        }

        this.disconnectReason = reason;

        PlayerDisconnectedEvent event = new PlayerDisconnectedEvent(this, reason);
        this.proxy.getEventManager().callEvent(event);

        if (this.connection != null && this.connection.isConnected()) {
            this.connection.disconnect(reason);
        }

        if (this.clientConnection != null) {
            this.clientConnection.getServerInfo().removeConnection(this.clientConnection);
            this.clientConnection.disconnect();
        }

        ClientConnection connection = this.getPendingConnection();
        if (connection != null) {
            connection.disconnect();
        }

        this.proxy.getPlayerManager().removePlayer(this);
        this.getLogger().info("[{}|{}] -> Upstream has disconnected: {}", this.getAddress(), this.getName(), reason);
    }

    public boolean sendToFallback(ServerInfo oldServer, String message) {
        return this.sendToFallback(oldServer, ReconnectReason.UNKNOWN, message);
    }

    /**
     * Send player to fallback server if any exists.
     *
     * @param oldServer server from which was player disconnected.
     * @param reason    disconnected reason.
     * @param message    disconnected message.
     * @return if connection to downstream was successful.
     */
    public boolean sendToFallback(ServerInfo oldServer, ReconnectReason reason, String message) {
        if (!this.isConnected()) {
            return false;
        }

        ServerInfo fallbackServer;
        try {
            fallbackServer = this.proxy.getReconnectHandler().getFallbackServer(this, oldServer, reason, message);
        } catch (Throwable t) {
            // A throwing reconnect handler must not abort failure handling; treat it as no fallback.
            this.getLogger().error("[" + this.getName() + "] ReconnectHandler getFallbackServer threw", t);
            return false;
        }
        if (fallbackServer != null && fallbackServer != this.getServerInfo()) {
            this.getLogger().debug("[{}] Connecting to fallback server {} with reason {}", this.getName(), fallbackServer.getServerName(), reason.getName());
            this.connect(fallbackServer);
            return true;
        }
        return false;
    }

    /**
     * Single entry point for every downstream failure signal: kicks, timeouts, channel exceptions
     * and channel closes all route here and are handled based on the connection's role.
     */
    public final void onDownstreamFailure(ClientConnection connection, ReconnectReason reason, String message) {
        Preconditions.checkNotNull(connection, "Connection can not be null!");

        // CLAIMED: mid-transfer target failure, the old downstream is already gone, fail the transfer.
        TransferCallback transferCallback = this.rewriteData.getTransferCallback();
        if (transferCallback != null && transferCallback.getConnection() == connection
                && transferCallback.getPhase() != TransferCallback.TransferPhase.RESET) {
            transferCallback.onTransferFailed(message);
            return;
        }

        // PENDING: failed before StartGame, the previous downstream still works, recover there.
        if (this.getPendingConnection() == connection) {
            this.onTransferFailure(connection, connection.getServerInfo(), reason, message);
            return;
        }

        // ACTIVE: the player's current downstream died, fail over.
        if (connection == this.clientConnection) {
            if (reason == ReconnectReason.EXCEPTION) {
                // The channel may still be open after an exception, close it before failing over.
                connection.disconnect();
            }
            this.onActiveDownstreamFailure(connection.getServerInfo(), reason, message);
            return;
        }

        // STALE: a discarded attempt, nothing to recover.
        connection.disconnect();
    }

    /**
     * Terminal for failures of the active downstream: fall back if possible, kick otherwise.
     * The per-reason branches preserve the messaging each failure source historically used.
     */
    private void onActiveDownstreamFailure(ServerInfo serverInfo, ReconnectReason reason, String message) {
        if (reason == ReconnectReason.UNKNOWN && (this.getPendingConnection() != null
                || !this.pendingServers.isEmpty() || this.disconnected.get())) {
            return; // a silent close while a transfer or disconnect is already in flight, let it finish
        }

        if (this.sendToFallback(serverInfo, reason, message)) {
            if (reason == ReconnectReason.EXCEPTION) {
                this.sendMessage(new TranslationContainer("waterdog.downstream.down", serverInfo.getServerName(), message));
            }
            return;
        }

        if (reason == ReconnectReason.SERVER_KICK) {
            this.disconnect(new TranslationContainer("waterdog.downstream.kicked", message));
        } else if (reason == ReconnectReason.TIMEOUT) {
            this.disconnect(new TranslationContainer("waterdog.downstream.down", serverInfo.getServerName(), "Timeout"));
        } else if (reason == ReconnectReason.UNKNOWN) {
            this.disconnect(new TranslationContainer("waterdog.downstream.down", serverInfo.getServerName(), "Disconnected"));
        } else {
            this.disconnect(new TranslationContainer("waterdog.downstream.down", serverInfo.getServerName(), message));
        }
    }

    public final void onDownstreamTimeout(ServerInfo serverInfo) {
        TransferCallback transferCallback = this.rewriteData.getTransferCallback();
        if (transferCallback != null && transferCallback.getTargetServer() == serverInfo
                && transferCallback.getPhase() != TransferCallback.TransferPhase.RESET) {
            this.onDownstreamFailure(transferCallback.getConnection(), ReconnectReason.TIMEOUT, "Downstream Timeout");
            return;
        }

        ClientConnection pendingConnection = this.getPendingConnection();
        if (pendingConnection != null && pendingConnection.getServerInfo() == serverInfo) {
            this.onDownstreamFailure(pendingConnection, ReconnectReason.TIMEOUT, "Downstream Timeout");
            return;
        }

        this.onActiveDownstreamFailure(serverInfo, ReconnectReason.TIMEOUT, "Downstream Timeout");
    }

    public final void onDownstreamDisconnected(ClientConnection connection) {
        this.getLogger().info("[" + connection.getSocketAddress() + "|" + this.getName() + "] -> Downstream [" +
                connection.getServerInfo().getServerName() + "] has disconnected");
        this.onDownstreamFailure(connection, ReconnectReason.UNKNOWN, "Downstream Disconnected");
    }

    /**
     * Sends a packet to the upstream connection (client)
     *
     * @param packet the packet to send
     */
    public void sendPacket(BedrockPacket packet) {
        if (this.connection != null && this.connection.isConnected()) {
            this.connection.sendPacket(packet);
        }
    }

    /**
     * Sends immediately packet to the upstream connection
     *
     * @param packet the packet to send
     */
    public void sendPacketImmediately(BedrockPacket packet) {
        if (this.connection != null && this.connection.isConnected()) {
            this.connection.sendPacketImmediately(packet);
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
     * Sends a chat message as this player, to the server he is currently connected to
     *
     * @param message the message to be sent
     */
    public void chat(String message) {
        if (message.trim().isEmpty()) {
            return; // Client wont accept empty string
        }

        ClientConnection connection = this.getDownstreamConnection();
        if (connection == null || !connection.isConnected()) {
            return; // This player is not connected to any server
        }

        if (message.charAt(0) == '/') {
            CommandRequestPacket packet = new CommandRequestPacket();
            packet.setCommand(message);
            packet.setCommandOriginData(new CommandOriginData(CommandOriginType.PLAYER, this.getUniqueId(), "", 0L));
            packet.setInternal(false);
            connection.sendPacket(packet);
            return;
        }

        TextPacket packet = new TextPacket();
        packet.setType(TextPacket.Type.CHAT);
        packet.setSourceName(this.getName());
        packet.setXuid(this.getXuid());
        packet.setMessage(message);
        connection.sendPacket(packet);
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
        packet.setXuid(this.getXuid());
        packet.setPlatformOnlineId("");
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
        packet.setXuid(this.getXuid());
        packet.setText("");
        packet.setPlatformOnlineId("");
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
        packet.setXuid(this.getXuid());
        packet.setPlatformOnlineId("");
        this.sendPacket(packet);
    }

    /**
     * Clears the title of the player
     */
    public void clearTitle() {
        SetTitlePacket packet = new SetTitlePacket();
        packet.setType(SetTitlePacket.Type.CLEAR);
        packet.setText("");
        packet.setXuid(this.getXuid());
        packet.setPlatformOnlineId("");
        this.sendPacket(packet);
    }

    /**
     * Resets all currently applied title settings
     */
    public void resetTitleSettings() {
        SetTitlePacket packet = new SetTitlePacket();
        packet.setType(SetTitlePacket.Type.RESET);
        packet.setText("");
        packet.setXuid(this.getXuid());
        packet.setPlatformOnlineId("");
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
        if (subtitle != null && !subtitle.trim().isEmpty()) {
            this.setSubtitle(subtitle);
        }
        this.setTitle((title == null || title.isEmpty()) ? " " : title);
    }

    /**
     * Sends a toast notification with a message to the player
     *
     * @param title the notification title
     * @param content the message content
     */
    public void sendToastMessage(String title, String content) {
        if (this.getProtocol().isBefore(ProtocolVersion.MINECRAFT_PE_1_19_0)) {
            return;
        }

        ToastRequestPacket packet = new ToastRequestPacket();
        packet.setTitle(title);
        packet.setContent(content);
        this.sendPacket(packet);
    }

    /**
     * Transfer player to another server using "slow" reconnect method
     *
     * @param serverInfo destination server
     */
    public void redirectServer(ServerInfo serverInfo) {
        Preconditions.checkNotNull(serverInfo, "Server info can not be null!");
        TransferPacket packet = new TransferPacket();
        packet.setAddress(serverInfo.getPublicAddress().getHostString());
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
        if (this.admin || permission.isEmpty()) {
            return true;
        }

        Permission perm = this.permissions.get(permission.toLowerCase());
        boolean result = perm != null && perm.getValue();

        PlayerPermissionCheckEvent event = new PlayerPermissionCheckEvent(this, permission, result);
        this.getProxy().getEventManager().callEvent(event);
        return event.hasPermission();
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
     * @return collection of assigned player permissions
     */
    public Collection<Permission> getPermissions() {
        return Collections.unmodifiableCollection(this.permissions.values());
    }

    @Override
    public boolean isPlayer() {
        return true;
    }

    public long getPing() {
        return this.connection.getPing();
    }

    /**
     * Safe way to get player's ServerInfo and to prevent NullPointer exception
     * Server connection may be null when player is connecting first time.
     *
     * @return ServerInfo if player is connected to downstream
     */
    public ServerInfo getServerInfo() {
        return this.clientConnection == null ? null : this.clientConnection.getServerInfo();
    }

    public InetSocketAddress getAddress() {
        return this.connection == null ? null : (InetSocketAddress) this.connection.getSocketAddress();
    }

    @Override
    public ProxyServer getProxy() {
        return this.proxy;
    }

    public MainLogger getLogger() {
        return this.proxy.getLogger();
    }

    public void setDownstreamConnection(ClientConnection connection) {
        this.clientConnection = connection;
        if (this.getPendingConnection() == connection) {
            this.setPendingConnection(null);
        }
    }

    public ClientConnection getDownstreamConnection() {
        return this.clientConnection;
    }

    public synchronized ClientConnection getPendingConnection() {
        return this.pendingConnection;
    }

    private synchronized void setPendingConnection(ClientConnection connection) {
        this.pendingConnection = connection;
    }

    /**
     * Clears the pending slot only if it is still owned by the given connection.
     *
     * @return true if the given connection was the pending one.
     */
    private synchronized boolean clearPendingConnection(ClientConnection connection) {
        if (this.pendingConnection != connection) {
            return false;
        }
        this.pendingConnection = null;
        return true;
    }

    public Collection<ServerInfo> getPendingServers() {
        return Collections.unmodifiableCollection(this.pendingServers);
    }

    public ServerInfo getConnectingServer() {
        return this.pendingConnection == null ? null : this.pendingConnection.getServerInfo();
    }

    public boolean isConnected() {
        return !this.disconnected.get() && this.connection != null && this.connection.isConnected();
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

    public Platform getDevicePlatform() {
        return this.loginData.getDevicePlatform();
    }

    public String getDeviceModel() {
        return this.loginData.getDeviceModel();
    }

    public String getDeviceId() {
        return this.loginData.getDeviceId();
    }

    public ProtocolVersion getProtocol() {
        return this.loginData.getProtocol();
    }

    public boolean canRewrite() {
        return this.canRewrite;
    }

    public boolean hasUpstreamBridge() {
        return this.hasUpstreamBridge;
    }

    public Collection<UUID> getPlayers() {
        return this.players;
    }

    public boolean acceptResourcePacks() {
        return this.acceptResourcePacks;
    }

    public boolean acceptItemComponentPacket() {
        return acceptItemComponentPacket;
    }

    public String getDisconnectReason() {
        return this.getDisconnectReason(String.class);
    }

    public <T extends CharSequence> T getDisconnectReason(Class<T> type) {
        return type.cast(this.disconnectReason);
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
