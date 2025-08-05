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

import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.command.Command;
import dev.waterdog.waterdogpe.event.defaults.DownstreamPacketReceivedEvent;
import dev.waterdog.waterdogpe.event.defaults.DownstreamPacketSentEvent;
import dev.waterdog.waterdogpe.event.defaults.UpstreamPacketSentEvent;
import dev.waterdog.waterdogpe.network.connection.client.ClientConnection;
import dev.waterdog.waterdogpe.network.protocol.ProtocolVersion;
import dev.waterdog.waterdogpe.network.protocol.Signals;
import dev.waterdog.waterdogpe.network.protocol.handler.ProxyPacketHandler;
import dev.waterdog.waterdogpe.network.protocol.rewrite.RewriteMaps;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodecHelper;
import org.cloudburstmc.protocol.bedrock.data.command.CommandData;
import org.cloudburstmc.protocol.bedrock.data.command.CommandEnumConstraint;
import org.cloudburstmc.protocol.bedrock.data.command.CommandEnumData;
import org.cloudburstmc.protocol.bedrock.data.command.CommandOverloadData;
import org.cloudburstmc.protocol.bedrock.data.command.CommandParam;
import org.cloudburstmc.protocol.bedrock.data.command.CommandParamData;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.netty.BedrockBatchWrapper;
import org.cloudburstmc.protocol.bedrock.packet.AvailableCommandsPacket;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.ChangeDimensionPacket;
import org.cloudburstmc.protocol.bedrock.packet.ChunkRadiusUpdatedPacket;
import org.cloudburstmc.protocol.bedrock.packet.ClientCacheMissResponsePacket;
import org.cloudburstmc.protocol.bedrock.packet.ItemComponentPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayStatusPacket;
import org.cloudburstmc.protocol.common.PacketSignal;
import org.cloudburstmc.protocol.common.SimpleDefinitionRegistry;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
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
    public PacketSignal handlePacket(BedrockPacket packet) {
        DownstreamPacketReceivedEvent event = new DownstreamPacketReceivedEvent(player, packet);
        ProxyServer.getInstance().getEventManager().callEvent(event);
        if(event.isCancelled()) return PacketSignal.HANDLED;

        return packet.handle(this);
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
                packet.getCommands().stream().filter(commandData -> commandData.getName().equalsIgnoreCase(command.getName())).findFirst().ifPresent(commandData -> packet.getCommands().remove(commandData));
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

        for(CommandData command : packet.getCommands()) {
            for(CommandOverloadData overload : command.getOverloads()) {
                for(CommandParamData param : overload.getOverloads()) {
                    if(param.getType() == null) {
                        param.setType(CommandParam.UNKNOWN);
                    }
                }
            }
        }

        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(ChunkRadiusUpdatedPacket packet) {
        this.player.getLoginData().getChunkRadius().setRadius(packet.getRadius());
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

    protected PacketSignal onPlayStatus(PlayStatusPacket packet, Consumer<String> failedTask, ClientConnection connection) {
        String message;
        switch (packet.getStatus()) {
            case LOGIN_SUCCESS -> {
                if (this.player.getProtocol().isAfterOrEqual(ProtocolVersion.MINECRAFT_PE_1_12)) {
                    connection.sendPacket(this.player.getLoginData().getCachePacket());
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

    protected void setItemDefinitions(Collection<ItemDefinition> definitions) {
        BedrockCodecHelper codecHelper = this.player.getConnection()
                .getPeer()
                .getCodecHelper();
        SimpleDefinitionRegistry.Builder<ItemDefinition> itemRegistry = SimpleDefinitionRegistry.builder();
        IntSet runtimeIds = new IntOpenHashSet();
        for (ItemDefinition definition : definitions) {
            if (runtimeIds.add(definition.getRuntimeId())) {
                itemRegistry.add(definition);
            } else {
                player.getLogger().warning("[{}|{}] has duplicate item definition: {}", this.player.getName(), this.connection.getServerInfo().getServerName(), definition);
            }
        }
        codecHelper.setItemDefinitions(itemRegistry.build());
    }
}
