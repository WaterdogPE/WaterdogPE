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

package dev.waterdog.network.upstream;

import com.nukkitx.protocol.bedrock.handler.BedrockPacketHandler;
import com.nukkitx.protocol.bedrock.packet.*;
import dev.waterdog.ProxyServer;
import dev.waterdog.event.defaults.PlayerChatEvent;
import dev.waterdog.event.defaults.PlayerResourcePackApplyEvent;
import dev.waterdog.packs.PackManager;
import dev.waterdog.player.ProxiedPlayer;
import dev.waterdog.utils.exceptions.CancelSignalException;

/**
 * Main handler for handling packets received from upstream.
 */
public class UpstreamHandler implements BedrockPacketHandler {

    private final ProxiedPlayer player;

    public UpstreamHandler(ProxiedPlayer player) {
        this.player = player;
    }

    @Override
    public boolean handle(ResourcePackClientResponsePacket packet) {
        if (!this.player.getProxy().getConfiguration().enabledResourcePacks() || !this.player.acceptResourcePacks()) {
            return false;
        }
        PackManager packManager = this.player.getProxy().getPackManager();

        switch (packet.getStatus()) {
            case REFUSED:
                this.player.disconnect("disconnectionScreen.noReason");
                break;
            case SEND_PACKS:
                for (String packIdVer : packet.getPackIds()) {
                    ResourcePackDataInfoPacket response = packManager.packInfoFromIdVer(packIdVer);
                    if (response == null) {
                        this.player.disconnect("disconnectionScreen.resourcePack");
                        break;
                    }
                    this.player.getUpstream().sendPacket(response);
                }
                break;
            case HAVE_ALL_PACKS:
                PlayerResourcePackApplyEvent event = new PlayerResourcePackApplyEvent(this.player, packManager.getStackPacket());
                this.player.getProxy().getEventManager().callEvent(event);
                this.player.getUpstream().sendPacket(event.getStackPacket());
                break;
            case COMPLETED:
                if (!this.player.hasUpstreamBridge()) {
                    this.player.initialConnect(); // First connection
                }
                break;
        }

        return this.cancel();
    }

    @Override
    public boolean handle(ResourcePackChunkRequestPacket packet) {
        if (!this.player.getProxy().getConfiguration().enabledResourcePacks() || !this.player.acceptResourcePacks()) {
            return false;
        }
        PackManager packManager = this.player.getProxy().getPackManager();
        ResourcePackChunkDataPacket response = packManager.packChunkDataPacket(packet.getPackId() + "_" + packet.getPackVersion(), packet);
        if (response == null) {
            this.player.disconnect("Unknown resource pack!");
            return this.cancel();
        }

        this.player.getUpstream().sendPacket(response);
        return this.cancel();
    }

    @Override
    public final boolean handle(RequestChunkRadiusPacket packet) {
        this.player.getRewriteData().setChunkRadius(packet);
        return false;
    }

    @Override
    public final boolean handle(PacketViolationWarningPacket packet) {
        this.player.getLogger().warning("Received " + packet.toString());
        return this.cancel();
    }

    @Override
    public final boolean handle(TextPacket packet) {
        PlayerChatEvent event = new PlayerChatEvent(this.player, packet.getMessage());
        ProxyServer.getInstance().getEventManager().callEvent(event);
        packet.setMessage(event.getMessage());

        if (event.isCancelled()) {
            throw CancelSignalException.CANCEL;
        }
        return true;
    }

    @Override
    public final boolean handle(CommandRequestPacket packet) {
        String message = packet.getCommand();
        if (this.player.getProxy().handlePlayerCommand(this.player, message)) {
            throw CancelSignalException.CANCEL;
        }
        return false;
    }

    /**
     * If connection has bridge we cancel packet to prevent sending it to downstream.
     *
     * @return true is we can't use CancelSignalException.
     */
    private boolean cancel() {
        if (this.player.hasUpstreamBridge()) {
            throw CancelSignalException.CANCEL;
        }
        return true;
    }

}
