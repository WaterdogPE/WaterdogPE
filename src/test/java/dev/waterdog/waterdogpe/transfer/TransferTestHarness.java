/*
 * Copyright 2026 WaterdogTEAM
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

package dev.waterdog.waterdogpe.transfer;

import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.event.Event;
import dev.waterdog.waterdogpe.event.EventManager;
import dev.waterdog.waterdogpe.logger.MainLogger;
import dev.waterdog.waterdogpe.network.connection.client.ClientConnection;
import dev.waterdog.waterdogpe.network.connection.codec.compression.CompressionType;
import dev.waterdog.waterdogpe.network.connection.handler.IReconnectHandler;
import dev.waterdog.waterdogpe.network.connection.peer.BedrockServerSession;
import dev.waterdog.waterdogpe.network.connection.peer.ProxiedBedrockPeer;
import dev.waterdog.waterdogpe.network.protocol.ProtocolVersion;
import dev.waterdog.waterdogpe.network.protocol.user.LoginData;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo;
import dev.waterdog.waterdogpe.player.PlayerManager;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import dev.waterdog.waterdogpe.scheduler.TaskHandler;
import dev.waterdog.waterdogpe.scheduler.WaterdogScheduler;
import dev.waterdog.waterdogpe.utils.config.proxy.ProxyConfig;
import dev.waterdog.waterdogpe.utils.types.TextContainer;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.netty.util.concurrent.Promise;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodecHelper;
import org.cloudburstmc.protocol.bedrock.packet.RequestChunkRadiusPacket;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Wires a real {@link ProxiedPlayer} against a fully mocked proxy so transfer failure modes
 * can be driven synchronously: events complete immediately, scheduled tasks are captured and
 * run on demand, and downstream connections are plain mocks.
 */
public class TransferTestHarness implements AutoCloseable {

    public final ProxyServer proxy = mock(ProxyServer.class);
    public final EventManager eventManager = mock(EventManager.class);
    public final WaterdogScheduler scheduler = mock(WaterdogScheduler.class);
    public final IReconnectHandler reconnectHandler = mock(IReconnectHandler.class);
    public final MainLogger logger = mock(MainLogger.class);
    public final ProxyConfig config = mock(ProxyConfig.class);
    public final PlayerManager playerManager = mock(PlayerManager.class);
    public final LoginData loginData = mock(LoginData.class);
    public final BedrockServerSession upstream = mock(BedrockServerSession.class);

    /**
     * Every event passed to the mocked EventManager, in call order.
     */
    public final List<Event> events = new ArrayList<>();
    /**
     * Translation keys of every message sent or shown to the player.
     */
    public final List<String> sentMessages = new ArrayList<>();
    /**
     * Every task passed to the mocked scheduler. Run them with {@link #runScheduledTasks()}.
     */
    public final List<Scheduled> scheduledTasks = new ArrayList<>();
    /**
     * Applied to every event before its future completes, e.g. to cancel it.
     */
    public Consumer<Event> eventInterceptor = event -> {
    };

    public final ProxiedPlayer player;

    public record Scheduled(Runnable task, int delayTicks, TaskHandler<?> handler) {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public TransferTestHarness() {
        when(this.proxy.getEventManager()).thenReturn(this.eventManager);
        when(this.proxy.getScheduler()).thenReturn(this.scheduler);
        when(this.proxy.getReconnectHandler()).thenReturn(this.reconnectHandler);
        when(this.proxy.getLogger()).thenReturn(this.logger);
        when(this.proxy.getConfiguration()).thenReturn(this.config);
        when(this.proxy.getPlayerManager()).thenReturn(this.playerManager);
        when(this.proxy.translate(any(TextContainer.class))).thenAnswer(invocation -> {
            TextContainer container = invocation.getArgument(0);
            this.sentMessages.add(container.getMessage());
            return container.getMessage();
        });
        when(this.config.getName()).thenReturn("TestProxy");

        when(this.eventManager.callEvent(any())).thenAnswer(invocation -> {
            Event event = invocation.getArgument(0);
            this.events.add(event);
            this.eventInterceptor.accept(event);
            return CompletableFuture.completedFuture(event);
        });

        when(this.scheduler.scheduleDelayed(any(Runnable.class), anyInt())).thenAnswer(invocation -> {
            TaskHandler<Runnable> handler = mock(TaskHandler.class);
            this.scheduledTasks.add(new Scheduled(invocation.getArgument(0), invocation.getArgument(1), handler));
            return handler;
        });

        ProxiedBedrockPeer peer = mock(ProxiedBedrockPeer.class);
        when(peer.getCodecHelper()).thenReturn(mock(BedrockCodecHelper.class));
        when(this.upstream.getPeer()).thenReturn(peer);
        when(this.upstream.isConnected()).thenReturn(true);

        when(this.loginData.getProtocol()).thenReturn(ProtocolVersion.MINECRAFT_PE_1_19_50);
        when(this.loginData.getDisplayName()).thenReturn("TestPlayer");
        when(this.loginData.getXuid()).thenReturn("123456");
        when(this.loginData.getChunkRadius()).thenReturn(new RequestChunkRadiusPacket());

        setProxyInstance(this.proxy);
        this.player = new ProxiedPlayer(this.proxy, this.upstream, CompressionType.ZLIB, this.loginData);
        ((AtomicBoolean) getField(this.player, "loginCompleted")).set(true);
    }

    @Override
    public void close() {
        setProxyInstance(null);
    }

    public ServerInfo newServer(String name) {
        ServerInfo info = mock(ServerInfo.class);
        when(info.getServerName()).thenReturn(name);
        return info;
    }

    public ClientConnection newDownstream(ServerInfo serverInfo) {
        ClientConnection connection = mock(ClientConnection.class);
        when(connection.getServerInfo()).thenReturn(serverInfo);
        when(connection.isConnected()).thenReturn(true);
        return connection;
    }

    /**
     * Stubs dialing the given server: the returned promise controls when (and how) the dial resolves.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Promise<ClientConnection> stubDial(ServerInfo serverInfo) {
        Promise<ClientConnection> promise = ImmediateEventExecutor.INSTANCE.newPromise();
        doReturn((Future) promise).when(serverInfo).createConnection(any());
        return promise;
    }

    public void setActiveDownstream(ClientConnection connection) {
        this.player.setDownstreamConnection(connection);
    }

    public void setPendingConnection(ClientConnection connection) {
        setField(this.player, "pendingConnection", connection);
    }

    /**
     * Runs every captured scheduled task once. Tasks scheduled while running are captured again.
     */
    public void runScheduledTasks() {
        List<Scheduled> tasks = new ArrayList<>(this.scheduledTasks);
        this.scheduledTasks.clear();
        for (Scheduled scheduled : tasks) {
            scheduled.task().run();
        }
    }

    public <T extends Event> List<T> events(Class<T> type) {
        return this.events.stream().filter(type::isInstance).map(type::cast).toList();
    }

    public static void setProxyInstance(ProxyServer proxy) {
        try {
            Field field = ProxyServer.class.getDeclaredField("instance");
            field.setAccessible(true);
            field.set(null, proxy);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to set ProxyServer instance", e);
        }
    }

    public static void setField(Object target, String name, Object value) {
        try {
            findField(target.getClass(), name).set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to set field " + name, e);
        }
    }

    public static Object getField(Object target, String name) {
        try {
            return findField(target.getClass(), name).get(target);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to read field " + name, e);
        }
    }

    private static Field findField(Class<?> clazz, String name) throws NoSuchFieldException {
        for (Class<?> current = clazz; current != null; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                // keep walking up
            }
        }
        throw new NoSuchFieldException(name);
    }
}