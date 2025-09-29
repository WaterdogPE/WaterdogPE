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

import com.nimbusds.jwt.SignedJWT;
import dev.waterdog.waterdogpe.network.connection.client.ClientConnection;
import dev.waterdog.waterdogpe.network.connection.handler.ReconnectReason;
import dev.waterdog.waterdogpe.network.protocol.registry.FakeDefinitionRegistry;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodecHelper;
import org.cloudburstmc.protocol.bedrock.packet.*;
import dev.waterdog.waterdogpe.event.defaults.InitialServerConnectedEvent;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo;
import dev.waterdog.waterdogpe.network.protocol.ProtocolVersion;
import dev.waterdog.waterdogpe.network.protocol.rewrite.BlockMap;
import dev.waterdog.waterdogpe.network.protocol.rewrite.BlockMapSimple;
import dev.waterdog.waterdogpe.network.protocol.rewrite.types.BlockPalette;
import dev.waterdog.waterdogpe.network.protocol.rewrite.types.RewriteData;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import dev.waterdog.waterdogpe.network.protocol.Signals;
import dev.waterdog.waterdogpe.utils.types.TranslationContainer;
import org.cloudburstmc.protocol.bedrock.util.EncryptionUtils;
import org.cloudburstmc.protocol.common.PacketSignal;

import javax.crypto.SecretKey;
import java.net.URI;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;
import java.util.concurrent.ThreadLocalRandom;

public class InitialHandler extends AbstractDownstreamHandler {

    public InitialHandler(ProxiedPlayer player, ClientConnection connection) {
        super(player, connection);
    }

    @Override
    public PacketSignal handle(PlayStatusPacket packet) {
        return this.onPlayStatus(packet, message -> {
            ServerInfo serverInfo = this.player.getServerInfo();
            if (!this.player.sendToFallback(serverInfo, ReconnectReason.TRANSFER_FAILED, message)) {
                this.player.disconnect(new TranslationContainer("waterdog.downstream.transfer.failed", serverInfo.getServerName(), message));
            }
        }, this.connection);
    }

    @Override
    public final PacketSignal handle(ServerToClientHandshakePacket packet) {
        try {
            SignedJWT saltJwt = SignedJWT.parse(packet.getJwt());
            URI x5u = saltJwt.getHeader().getX509CertURL();
            ECPublicKey serverKey = EncryptionUtils.parseKey(x5u.toASCIIString());
            SecretKey key = EncryptionUtils.getSecretKey(
                    this.player.getLoginData().getKeyPair().getPrivate(),
                    serverKey,
                    Base64.getDecoder().decode(saltJwt.getJWTClaimsSet().getStringClaim("salt"))
            );
            this.connection.enableEncryption(key);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to enable encryption", e);
        }

        ClientToServerHandshakePacket clientToServerHandshake = new ClientToServerHandshakePacket();
        this.connection.sendPacket(clientToServerHandshake);
        return Signals.CANCEL;
    }

    @Override
    public final PacketSignal handle(ResourcePacksInfoPacket packet) {
        if (!this.player.getProxy().getConfiguration().enableResourcePacks() || !this.player.acceptResourcePacks()) {
            return PacketSignal.UNHANDLED;
        }
        ResourcePackClientResponsePacket response = new ResourcePackClientResponsePacket();
        response.setStatus(ResourcePackClientResponsePacket.Status.HAVE_ALL_PACKS);
        this.connection.sendPacket(response);
        return Signals.CANCEL;
    }

    @Override
    public final PacketSignal handle(ResourcePackStackPacket packet) {
        if (!this.player.getProxy().getConfiguration().enableResourcePacks() || !this.player.acceptResourcePacks()) {
            return PacketSignal.UNHANDLED;
        }
        ResourcePackClientResponsePacket response = new ResourcePackClientResponsePacket();
        response.setStatus(ResourcePackClientResponsePacket.Status.COMPLETED);
        this.connection.sendPacket(response);
        return Signals.CANCEL;
    }

    @Override
    public final PacketSignal handle(StartGamePacket packet) {
        RewriteData rewriteData = this.player.getRewriteData();
        rewriteData.setOriginalEntityId(packet.getRuntimeEntityId());
        rewriteData.setEntityId(ThreadLocalRandom.current().nextInt(10000, 15000));
        rewriteData.setGameRules(packet.getGamerules());
        rewriteData.setDimension(packet.getDimensionId());
        rewriteData.setSpawnPosition(packet.getPlayerPosition());
        packet.setRuntimeEntityId(rewriteData.getEntityId());
        packet.setUniqueEntityId(rewriteData.getEntityId());
        packet.setLevelName(rewriteData.getProxyName());

        // Starting with 419 server does not send vanilla blocks to client
        if (this.player.getProtocol().isBeforeOrEqual(ProtocolVersion.MINECRAFT_PE_1_16_20)) {
            BlockPalette palette = BlockPalette.getPalette(packet.getBlockPalette(), this.player.getProtocol());
            rewriteData.setBlockPalette(palette);
            rewriteData.setBlockPaletteRewrite(palette.createRewrite(palette));
            this.player.getRewriteMaps().setBlockMap(new BlockMap(this.player));
        } else {
            rewriteData.setBlockProperties(packet.getBlockProperties());
            this.player.getRewriteMaps().setBlockMap(new BlockMapSimple(this.player));
        }

        BedrockCodecHelper codecHelper = this.player.getConnection()
                .getPeer()
                .getCodecHelper();
        // Setup item registry. After 1.21.60 these are sent with ItemComponentPacket instead.
        if (this.player.getProtocol().isBeforeOrEqual(ProtocolVersion.MINECRAFT_PE_1_21_50)) {
            setItemDefinitions(packet.getItemDefinitions());
        }
        // Setup block registry
        codecHelper.setBlockDefinitions(FakeDefinitionRegistry.createBlockRegistry());
        // Enable runtimeId rewrite
        this.player.setCanRewrite(true);

        this.connection.getServerInfo().addConnection(this.connection);
        this.player.setDownstreamConnection(this.connection);

        this.connection.setPacketHandler(new ConnectedDownstreamHandler(this.player, this.connection));
        this.player.getProxy().getEventManager().callEvent(new InitialServerConnectedEvent(this.player, this.connection));
        return PacketSignal.HANDLED;
    }
}
