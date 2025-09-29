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
import dev.waterdog.waterdogpe.network.protocol.handler.TransferCallback;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.ScoreInfo;
import org.cloudburstmc.protocol.bedrock.packet.*;
import dev.waterdog.waterdogpe.event.defaults.ServerTransferEvent;
import dev.waterdog.waterdogpe.network.protocol.ProtocolVersion;
import dev.waterdog.waterdogpe.network.protocol.rewrite.types.BlockPalette;
import dev.waterdog.waterdogpe.network.protocol.rewrite.types.RewriteData;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import dev.waterdog.waterdogpe.network.protocol.Signals;
import dev.waterdog.waterdogpe.utils.types.TranslationContainer;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import org.cloudburstmc.protocol.bedrock.util.EncryptionUtils;
import org.cloudburstmc.protocol.common.PacketSignal;

import javax.crypto.SecretKey;
import java.net.URI;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;
import java.util.Collection;
import java.util.UUID;

import static dev.waterdog.waterdogpe.network.protocol.user.PlayerRewriteUtils.*;

@Log4j2
public class SwitchDownstreamHandler extends AbstractDownstreamHandler {

    public SwitchDownstreamHandler(ProxiedPlayer player, ClientConnection connection) {
        super(player, connection);
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
        ResourcePackClientResponsePacket response = new ResourcePackClientResponsePacket();
        response.setStatus(ResourcePackClientResponsePacket.Status.HAVE_ALL_PACKS);
        this.connection.sendPacket(response);
        return Signals.CANCEL;
    }

    @Override
    public final PacketSignal handle(ResourcePackStackPacket packet) {
        ResourcePackClientResponsePacket response = new ResourcePackClientResponsePacket();
        response.setStatus(ResourcePackClientResponsePacket.Status.COMPLETED);
        this.connection.sendPacket(response);
        return Signals.CANCEL;
    }

    @Override
    public PacketSignal handle(PlayStatusPacket packet) {
        return this.onPlayStatus(packet, message -> {
            this.connection.disconnect();
            this.player.sendMessage(new TranslationContainer("waterdog.downstream.transfer.failed", this.connection.getServerInfo().getServerName(), message));
        }, this.connection);
    }

    @Override
    public final PacketSignal handle(StartGamePacket packet) {
        RewriteData rewriteData = this.player.getRewriteData();
        rewriteData.setOriginalEntityId(packet.getRuntimeEntityId());
        rewriteData.setGameRules(packet.getGamerules());
        rewriteData.setSpawnPosition(packet.getPlayerPosition());
        rewriteData.setRotation(packet.getRotation());

        if (this.player.getProtocol().isBeforeOrEqual(ProtocolVersion.MINECRAFT_PE_1_16_20)) {
            BlockPalette palette = BlockPalette.getPalette(packet.getBlockPalette(), this.player.getProtocol());
            rewriteData.setBlockPaletteRewrite(palette.createRewrite(rewriteData.getBlockPalette()));
        } else {
            rewriteData.setBlockProperties(packet.getBlockProperties());
        }

        if (!this.player.isConnected()) {
            this.connection.disconnect();
            this.player.disconnect("transfer disconnected");
            return Signals.CANCEL;
        }

        if (rewriteData.getTransferCallback() != null && rewriteData.getTransferCallback().getPhase() != TransferCallback.TransferPhase.RESET) {
            this.connection.disconnect();
            String serverName = this.connection.getServerInfo().getServerName();
            this.player.sendMessage(new TranslationContainer("waterdog.downstream.connecting", serverName));
            log.warn("[{}] Aborted server transfer to {} because player is already being transferred!", this.player.getName(), serverName);
            return Signals.CANCEL;
        }

        ClientConnection oldConnection = this.player.getDownstreamConnection();
        oldConnection.getServerInfo().removeConnection(oldConnection);
        oldConnection.disconnect();
        this.player.setDownstreamConnection(this.connection);
        this.connection.getServerInfo().addConnection(this.connection);
        this.player.setAcceptPlayStatus(true);

        ServerTransferEvent event = new ServerTransferEvent(this.player, oldConnection.getServerInfo(), this.connection.getServerInfo());
        this.player.getProxy().getEventManager().callEvent(event);

        LongSet blobs = this.player.getChunkBlobs();
        if (this.player.getProtocol().isBefore(ProtocolVersion.MINECRAFT_PE_1_18_30) &&
                this.player.getLoginData().getCachePacket().isSupported() && !blobs.isEmpty()) {
            injectChunkCacheBlobs(this.player.getConnection(), blobs);
        }
        this.player.getChunkBlobs().clear();

        Long2LongMap entityLinks = this.player.getEntityLinks();
        for (Long2LongMap.Entry entry : entityLinks.long2LongEntrySet()) {
            injectRemoveEntityLink(this.player.getConnection(), entry.getLongKey(), entry.getLongValue());
        }
        entityLinks.clear();

        LongSet bossbars = this.player.getBossbars();
        for (long bossbarId : bossbars) {
            injectRemoveBossbar(this.player.getConnection(), bossbarId);
        }
        bossbars.clear();

        Collection<UUID> playerList = this.player.getPlayers();
        injectRemoveAllPlayers(this.player.getConnection(), playerList);
        playerList.clear();

        LongSet entities = this.player.getEntities();
        for (long entityId : entities) {
            injectRemoveEntity(this.player.getConnection(), entityId);
        }
        entities.clear();

        Long2ObjectMap<ScoreInfo> scoreInfos = this.player.getScoreInfos();
        injectRemoveScoreInfos(this.player.getConnection(), scoreInfos);
        scoreInfos.clear();

        ObjectSet<String> scoreboards = this.player.getScoreboards();
        for (String scoreboard : scoreboards) {
            injectRemoveObjective(this.player.getConnection(), scoreboard);
        }
        scoreboards.clear();

        injectRemoveAllEffects(this.player.getConnection(), rewriteData.getEntityId(), this.player.getProtocol());
        injectClearWeather(this.player.getConnection());

        injectGameMode(this.player.getConnection(), packet.getPlayerGameType());
        injectSetDifficulty(this.player.getConnection(), packet.getDifficulty());
        injectGameRules(this.player.getConnection(), packet.getGamerules());

        this.connection.sendPacket(this.player.getLoginData().getChunkRadius());

        // Client does not accept ChangeDimensionPacket when dimension is same as current dimension.
        // If we transfer between same dimensions we are attempting to do dimension change sequence which uses 2 dim changes
        // After client successfully changes dimension we receive PlayerActionPacket#DIMENSION_CHANGE_SUCCESS and continue in transfer
        int newDimension = determineDimensionId(rewriteData.getDimension(), packet.getDimensionId());

        TransferCallback transferCallback = new TransferCallback(this.player, this.connection, oldConnection.getServerInfo(), packet.getDimensionId());
        rewriteData.setDimension(newDimension);
        rewriteData.setTransferCallback(transferCallback);

        boolean fastTransfer = event.isTransferScreenAllowed() && newDimension != packet.getDimensionId();
        if (fastTransfer) {
            Vector3f fakePosition = packet.getPlayerPosition().add(2000, 0, 2000);
            injectPosition(this.player.getConnection(), fakePosition, packet.getRotation(), rewriteData.getEntityId());
            this.player.getConnection().setTransferQueueActive(true);
            injectDimensionChange(this.player.getConnection(), newDimension, fakePosition,
                    rewriteData.getEntityId(), player.getProtocol(), true);
            // Force client to exit first dim screen after one second
            this.player.getProxy().getScheduler().scheduleDelayed(() -> {
                PlayStatusPacket statusPacket = new PlayStatusPacket();
                statusPacket.setStatus(PlayStatusPacket.Status.PLAYER_SPAWN);
                this.player.getConnection().sendPacketImmediately(statusPacket);
            }, 40);
        } else if (newDimension == packet.getDimensionId()) {
            // Transfer between different dimensions
            injectPosition(this.player.getConnection(), packet.getPlayerPosition(), packet.getRotation(), rewriteData.getEntityId());
            injectDimensionChange(this.player.getConnection(), newDimension, packet.getPlayerPosition(),
                    rewriteData.getEntityId(), player.getProtocol(), false);
            transferCallback.onDimChangeSuccess(); // Simulate two dim-change behaviour
        } else {
            injectPosition(this.player.getConnection(), packet.getPlayerPosition(), packet.getRotation(), rewriteData.getEntityId());
            rewriteData.setDimension(packet.getDimensionId());
            transferCallback.onDimChangeSuccess();
            transferCallback.onDimChangeSuccess();
        }
        return Signals.CANCEL;
    }

    @Override
    public PacketSignal handle(DisconnectPacket packet) {
        TransferCallback transferCallback = this.player.getRewriteData().getTransferCallback();
        if (transferCallback != null) {
            // Player was already disconnected from old downstream
            transferCallback.onTransferFailed();
            return Signals.CANCEL;
        }

        this.connection.disconnect();
        this.player.sendMessage(new TranslationContainer("waterdog.downstream.transfer.failed", this.connection.getServerInfo().getServerName(), packet.getKickMessage()));
        return Signals.CANCEL;
    }
}
