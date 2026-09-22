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

package dev.waterdog.waterdogpe.network.protocol.handler.downstream;

import dev.waterdog.waterdogpe.command.Command;
import dev.waterdog.waterdogpe.network.connection.client.ClientConnection;
import dev.waterdog.waterdogpe.network.protocol.ProtocolVersion;
import dev.waterdog.waterdogpe.network.protocol.handler.ProxyPacketHandler;
import dev.waterdog.waterdogpe.network.protocol.handler.TransferCallback;
import dev.waterdog.waterdogpe.network.protocol.registry.FakeDefinitionRegistry;
import dev.waterdog.waterdogpe.network.protocol.rewrite.RewriteMaps;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import dev.waterdog.waterdogpe.network.protocol.Signals;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodecHelper;
import org.cloudburstmc.protocol.bedrock.data.camera.CameraPreset;
import org.cloudburstmc.protocol.bedrock.data.command.CommandData;
import org.cloudburstmc.protocol.bedrock.data.command.CommandEnumConstraint;
import org.cloudburstmc.protocol.bedrock.data.command.CommandEnumData;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.definitions.SimpleNamedDefinition;
import org.cloudburstmc.protocol.bedrock.netty.BedrockBatchWrapper;
import org.cloudburstmc.protocol.bedrock.packet.*;
import org.cloudburstmc.protocol.common.NamedDefinition;
import org.cloudburstmc.protocol.common.PacketSignal;
import org.cloudburstmc.protocol.common.SimpleDefinitionRegistry;

import java.util.*;
import java.util.function.Consumer;

import static dev.waterdog.waterdogpe.network.protocol.Signals.mergeSignals;

public abstract class AbstractDownstreamHandler implements ProxyPacketHandler {

    protected final ClientConnection connection;
    protected final ProxiedPlayer player;

    public AbstractDownstreamHandler(ProxiedPlayer player, ClientConnection connection) {
        this.player = player;
        this.connection = connection;
    }

    @Override
    public PacketSignal handle(PlayStatusPacket packet) {
        if (packet.getStatus() != PlayStatusPacket.Status.PLAYER_SPAWN) {
            return PacketSignal.UNHANDLED;
        }
        TransferCallback transferCallback = player.getRewriteData().getTransferCallback();
        if (transferCallback != null && transferCallback.getConnection() == this.connection) {
            transferCallback.onPlayStatus();
        }
        return PacketSignal.UNHANDLED;
    }

    /**
     * A server the player is being handed to runs its own spawn sequence, and a Bedrock Dedicated
     * Server opens it with a respawn handshake: it asks for a spawn point and waits for the client
     * to answer that it is ready before it starts ticking the player.
     *
     * <p>The client has no reason to answer. It is mid session, it never died, and it was never told
     * it changed servers, so it ignores the request - and forwarding it would risk showing a death
     * screen for a death that did not happen. The proxy answers on its behalf instead, or the player
     * ends up in a world where they can walk and break blocks and the server registers none of it.</p>
     *
     * <p>Only while a transfer to this connection is still in flight: a respawn on the server the
     * player already plays on is a real death and belongs to the client.</p>
     */
    @Override
    public PacketSignal handle(RespawnPacket packet) {
        if (packet.getState() != RespawnPacket.State.SERVER_SEARCHING) {
            return PacketSignal.UNHANDLED;
        }

        TransferCallback transferCallback = this.player.getRewriteData().getTransferCallback();
        if (transferCallback == null || transferCallback.getConnection() != this.connection) {
            return PacketSignal.UNHANDLED;
        }

        RespawnPacket response = new RespawnPacket();
        // Still the id downstream knows: the rewrite runs after the handlers.
        response.setRuntimeEntityId(packet.getRuntimeEntityId());
        response.setPosition(packet.getPosition());
        response.setState(RespawnPacket.State.CLIENT_READY);
        this.connection.sendPacket(response);
        return Signals.CANCEL;
    }

    @Override
    public PacketSignal handle(ItemComponentPacket packet) {
        if (!this.player.acceptItemComponentPacket()) {
            return Signals.CANCEL;
        }
        player.setAcceptItemComponentPacket(false);
        if (this.player.getProtocol().isAfterOrEqual(ProtocolVersion.MINECRAFT_PE_1_21_60)) {
            setItemDefinitions(packet.getItems());
        }
        return PacketSignal.UNHANDLED;
    }

    @Override
    public void sendProxiedBatch(BedrockBatchWrapper batch) {
        ClientConnection current = this.player.getDownstreamConnection();
        if (current != null && this.connection != current) {
            // Noop. Drop batches from a downstream that is no longer the player's active one.
            // Null check is for the initial connection.
            return;
        }
        if (this.player.getConnection().isConnected()) {
            this.player.getConnection().sendPacket(batch.retain());
        }
    }

    @Override
    public PacketSignal doPacketRewrite(BedrockPacket packet) {
        RewriteMaps rewriteMaps = this.player.getRewriteMaps();
        if (rewriteMaps.getBlockMap() != null) {
            return mergeSignals(rewriteMaps.getBlockMap().doRewrite(packet),
                    ProxyPacketHandler.super.doPacketRewrite(packet));
        }
        return ProxyPacketHandler.super.doPacketRewrite(packet);
    }

    @Override
    public PacketSignal handle(AvailableCommandsPacket packet) {
        if (!this.player.getProxy().getConfiguration().injectCommands()) {
            return PacketSignal.UNHANDLED;
        }
        int sizeBefore = packet.getCommands().size();

        for (Command command : this.player.getProxy().getCommandMap().getCommands().values()) {
            if (command.getPermission() == null || this.player.hasPermission(command.getPermission())) {
                packet.getCommands().add(command.getCommandData());
            }
        }

        if (packet.getCommands().size() == sizeBefore) {
            return PacketSignal.UNHANDLED;
        }

        // Some server commands are missing aliases, which protocol lib doesn't like
        ListIterator<CommandData> iterator = packet.getCommands().listIterator();
        while (iterator.hasNext()) {
            CommandData command = iterator.next();
            if (command.getAliases() != null) {
                continue;
            }

            Map<String, Set<CommandEnumConstraint>> aliases = new LinkedHashMap<>();
            aliases.put(command.getName(), EnumSet.of(CommandEnumConstraint.ALLOW_ALIASES));

            iterator.set(new CommandData(command.getName(),
                    command.getDescription(),
                    command.getFlags(),
                    command.getPermission(),
                    new CommandEnumData(command.getName() + "_aliases", aliases, false),
                    Collections.emptyList(),
                    command.getOverloads()));
        }
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(ChunkRadiusUpdatedPacket packet) {
        this.player.getLoginData().setChunkRadius(packet.getRadius());
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(ChangeDimensionPacket packet) {
        this.player.getRewriteData().setDimension(packet.getDimension());
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(ClientCacheMissResponsePacket packet) {
        if (this.player.getProtocol().isBefore(ProtocolVersion.MINECRAFT_PE_1_18_30)) {
            this.player.getChunkBlobs().removeAll(packet.getBlobs().keySet());
        }
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(CameraPresetsPacket packet) {
        setCameraPresetDefinitions(packet.getPresets());
        return PacketSignal.UNHANDLED;
    }

    protected PacketSignal onPlayStatus(PlayStatusPacket packet, Consumer<String> failedTask, ClientConnection connection) {
        String message;
        switch (packet.getStatus()) {
            case LOGIN_SUCCESS -> {
                if (this.player.getProtocol().isAfterOrEqual(ProtocolVersion.MINECRAFT_PE_1_12)) {
                    ClientCacheStatusPacket cachePacket = new ClientCacheStatusPacket();
                    cachePacket.setSupported(this.player.getLoginData().isCacheSupported());
                    connection.sendPacket(cachePacket);
                }
                return Signals.CANCEL;
            }
            case LOGIN_FAILED_CLIENT_OLD, LOGIN_FAILED_SERVER_OLD -> message = "Incompatible version";
            case FAILED_SERVER_FULL_SUB_CLIENT -> message = "Server is full";
            default -> {
                return PacketSignal.UNHANDLED;
            }
        }

        failedTask.accept(message);
        return Signals.CANCEL;
    }

    @Override
    public RewriteMaps getRewriteMaps() {
        return this.player.getRewriteMaps();
    }

    @Override
    public ClientConnection getConnection() {
        return connection;
    }

    protected static final String BLOCKING_ID = "minecraft:shield";

    protected void setItemDefinitions(Collection<ItemDefinition> definitions) {
        BedrockCodecHelper codecHelper = this.player.getConnection()
            .getPeer()
            .getCodecHelper();
        FakeDefinitionRegistry<ItemDefinition> itemRegistry = FakeDefinitionRegistry.createItemRegistry();
        for (ItemDefinition definition : definitions) {
            if (definition.getIdentifier().equals(BLOCKING_ID)) {
                itemRegistry.getRuntimeMap().put(definition.getRuntimeId(), definition);
                break;
            }
        }
        codecHelper.setItemDefinitions(itemRegistry);
    }

    protected void setCameraPresetDefinitions(Collection<CameraPreset> presets) {
        BedrockCodecHelper codecHelper = this.player.getConnection()
                .getPeer()
                .getCodecHelper();
        SimpleDefinitionRegistry.Builder<NamedDefinition> registry = SimpleDefinitionRegistry.builder();
        int id = 0;
        for (CameraPreset preset : presets) {
            registry.add(new SimpleNamedDefinition(preset.getIdentifier(), id++));
        }
        codecHelper.setCameraPresetDefinitions(registry.build());
    }
}
