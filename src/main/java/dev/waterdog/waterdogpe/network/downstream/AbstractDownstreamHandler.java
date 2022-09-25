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

package dev.waterdog.waterdogpe.network.downstream;

import com.nukkitx.protocol.bedrock.handler.BedrockPacketHandler;
import com.nukkitx.protocol.bedrock.packet.*;
import dev.waterdog.waterdogpe.command.Command;
import dev.waterdog.waterdogpe.network.protocol.ProtocolVersion;
import dev.waterdog.waterdogpe.network.session.CompressionAlgorithm;
import dev.waterdog.waterdogpe.network.session.DownstreamClient;
import dev.waterdog.waterdogpe.network.session.DownstreamSession;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import dev.waterdog.waterdogpe.utils.exceptions.CancelSignalException;

import java.util.function.Consumer;

public abstract class AbstractDownstreamHandler implements BedrockPacketHandler {

    protected final DownstreamClient client;
    protected final ProxiedPlayer player;

    public AbstractDownstreamHandler(ProxiedPlayer player, DownstreamClient client) {
        this.player = player;
        this.client = client;
    }

    // TODO: move this to separate handler once more API breaking changes are done
    @Override
    public boolean handle(NetworkSettingsPacket packet) {
        CompressionAlgorithm compression = CompressionAlgorithm.fromBedrockCompression(packet.getCompressionAlgorithm());
        this.client.getSession().setCompression(compression);
        this.player.getLoginData().doLogin(this.client.getSession());
        throw CancelSignalException.CANCEL;
    }

    @Override
    public boolean handle(AvailableCommandsPacket packet) {
        if (!this.player.getProxy().getConfiguration().injectCommands()) {
            return false;
        }
        int sizeBefore = packet.getCommands().size();

        for (Command command : this.player.getProxy().getCommandMap().getCommands().values()) {
            if (command.getPermission() == null || this.player.hasPermission(command.getPermission())) {
                packet.getCommands().add(command.getData());
            }
        }
        return packet.getCommands().size() > sizeBefore;
    }

    @Override
    public boolean handle(ChunkRadiusUpdatedPacket packet) {
        this.player.getLoginData().getChunkRadius().setRadius(packet.getRadius());
        return false;
    }

    @Override
    public boolean handle(ChangeDimensionPacket packet) {
        this.player.getRewriteData().setDimension(packet.getDimension());
        return false;
    }

    @Override
    public boolean handle(ClientCacheMissResponsePacket packet) {
        if (this.player.getProtocol().isBefore(ProtocolVersion.MINECRAFT_PE_1_18_30)) {
            this.player.getChunkBlobs().removeAll(packet.getBlobs().keySet());
        }
        return false;
    }

    protected boolean onPlayStatus(PlayStatusPacket packet, Consumer<String> failedTask, DownstreamSession downstream) {
        String message;
        switch (packet.getStatus()) {
            case LOGIN_SUCCESS:
                if (this.player.getProtocol().isAfterOrEqual(ProtocolVersion.MINECRAFT_PE_1_12)) {
                    downstream.sendPacket(this.player.getLoginData().getCachePacket());
                }
                throw CancelSignalException.CANCEL;
            case LOGIN_FAILED_CLIENT_OLD:
            case LOGIN_FAILED_SERVER_OLD:
                message = "Incompatible version";
                break;
            case FAILED_SERVER_FULL_SUB_CLIENT:
                message = "Server is full";
                break;
            default:
                return false;
        }

        failedTask.accept(message);
        throw CancelSignalException.CANCEL;
    }
}
