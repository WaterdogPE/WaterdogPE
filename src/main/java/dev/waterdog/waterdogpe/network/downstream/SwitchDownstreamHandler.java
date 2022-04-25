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

import com.nimbusds.jwt.SignedJWT;
import com.nukkitx.protocol.bedrock.data.ScoreInfo;
import com.nukkitx.protocol.bedrock.packet.*;
import com.nukkitx.protocol.bedrock.util.EncryptionUtils;
import dev.waterdog.waterdogpe.network.protocol.ProtocolVersion;
import dev.waterdog.waterdogpe.network.rewrite.types.BlockPalette;
import dev.waterdog.waterdogpe.network.rewrite.types.RewriteData;
import dev.waterdog.waterdogpe.network.session.*;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import dev.waterdog.waterdogpe.utils.exceptions.CancelSignalException;
import dev.waterdog.waterdogpe.utils.types.TranslationContainer;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;

import javax.crypto.SecretKey;
import java.net.URI;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;
import java.util.Collection;
import java.util.UUID;

import static dev.waterdog.waterdogpe.player.PlayerRewriteUtils.*;

public class SwitchDownstreamHandler extends AbstractDownstreamHandler {

    public SwitchDownstreamHandler(ProxiedPlayer player, DownstreamClient client) {
        super(player, client);
    }

    public DownstreamSession getDownstream() {
        return this.client.getSession();
    }

    @Override
    public final boolean handle(ServerToClientHandshakePacket packet) {
        try {
            SignedJWT saltJwt = SignedJWT.parse(packet.getJwt());
            URI x5u = saltJwt.getHeader().getX509CertURL();
            ECPublicKey serverKey = EncryptionUtils.generateKey(x5u.toASCIIString());
            SecretKey key = EncryptionUtils.getSecretKey(
                    this.player.getLoginData().getKeyPair().getPrivate(),
                    serverKey,
                    Base64.getDecoder().decode(saltJwt.getJWTClaimsSet().getStringClaim("salt"))
            );
            this.getDownstream().enableEncryption(key);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        ClientToServerHandshakePacket clientToServerHandshake = new ClientToServerHandshakePacket();
        this.getDownstream().sendPacketImmediately(clientToServerHandshake);
        throw CancelSignalException.CANCEL;
    }

    @Override
    public final boolean handle(ResourcePacksInfoPacket packet) {
        ResourcePackClientResponsePacket response = new ResourcePackClientResponsePacket();
        response.setStatus(ResourcePackClientResponsePacket.Status.HAVE_ALL_PACKS);
        this.getDownstream().sendPacketImmediately(response);
        throw CancelSignalException.CANCEL;
    }

    @Override
    public final boolean handle(ResourcePackStackPacket packet) {
        ResourcePackClientResponsePacket response = new ResourcePackClientResponsePacket();
        response.setStatus(ResourcePackClientResponsePacket.Status.COMPLETED);
        this.getDownstream().sendPacketImmediately(response);
        throw CancelSignalException.CANCEL;
    }

    @Override
    public boolean handle(PlayStatusPacket packet) {
        return this.onPlayStatus(packet, message -> {
            this.client.close();
            this.player.setPendingConnection(null);
            this.player.sendMessage(new TranslationContainer("waterdog.downstream.transfer.failed", this.client.getServerInfo().getServerName(), message));
        }, this.getDownstream());
    }

    @Override
    public final boolean handle(StartGamePacket packet) {
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

        DownstreamClient oldDownstream = this.player.getDownstream();
        oldDownstream.getServerInfo().removePlayer(this.player);
        oldDownstream.close();

        Collection<UUID> playerList = this.player.getPlayers();
        injectRemoveAllPlayers(this.player.getUpstream(), playerList);
        playerList.clear();

        LongSet bossbars = this.player.getBossbars();
        for (long bossbarId : bossbars) {
            injectRemoveBossbar(this.player.getUpstream(), bossbarId);
        }
        bossbars.clear();

        Long2LongMap entityLinks = this.player.getEntityLinks();
        for (Long2LongMap.Entry entry : entityLinks.long2LongEntrySet()) {
            injectRemoveEntityLink(this.player.getUpstream(), entry.getLongKey(), entry.getLongValue());
        }
        entityLinks.clear();

        LongSet entities = this.player.getEntities();
        for (long entityId : entities) {
            injectRemoveEntity(this.player.getUpstream(), entityId);
        }
        entities.clear();

        Long2ObjectMap<ScoreInfo> scoreInfos = this.player.getScoreInfos();
        injectRemoveScoreInfos(this.player.getUpstream(), scoreInfos);
        scoreInfos.clear();

        ObjectSet<String> scoreboards = this.player.getScoreboards();
        for (String scoreboard : scoreboards) {
            injectRemoveObjective(this.player.getUpstream(), scoreboard);
        }
        scoreboards.clear();

        injectGameMode(this.player.getUpstream(), packet.getPlayerGameType());
        injectSetDifficulty(this.player.getUpstream(), packet.getDifficulty());
        injectPosition(this.player.getUpstream(), rewriteData.getSpawnPosition(), rewriteData.getRotation(), rewriteData.getEntityId());
        this.getDownstream().sendPacket(this.player.getLoginData().getChunkRadius());

        // Client does not accept ChangeDimensionPacket when dimension is same as current dimension.
        // If we transfer between same dimensions we are attempting to do dimension change sequence which uses 2 dim changes
        // After client successfully changes dimension we receive PlayerActionPacket#DIMENSION_CHANGE_SUCCESS and continue in transfer
        int newDimension = determineDimensionId(rewriteData.getDimension(), packet.getDimensionId());
        TransferCallback transferCallback = new TransferCallback(this.player, this.client, packet.getDimensionId());

        rewriteData.setDimension(newDimension);
        rewriteData.setTransferCallback(transferCallback);
        this.player.setDimensionChangeState(TransferCallback.TRANSFER_PHASE_1);
        injectDimensionChange(this.player.getUpstream(), newDimension, packet.getPlayerPosition(), this.player.getProtocol());

        if (newDimension == packet.getDimensionId()) {
            // Transfer between different dimensions
            // Simulate two dim-change behaviour
            transferCallback.onDimChangeSuccess();
        }  else {
            // Force client to exit first dim screen after one second
            PlayStatusPacket status = new PlayStatusPacket();
            status.setStatus(PlayStatusPacket.Status.PLAYER_SPAWN);
            this.player.getProxy().getScheduler().scheduleDelayed(() -> this.player.sendPacket(status), 20);
        }
        this.getDownstream().onServerConnected(player);
        throw CancelSignalException.CANCEL;
    }

    @Override
    public boolean handle(DisconnectPacket packet) {
        TransferCallback transferCallback = this.player.getRewriteData().getTransferCallback();
        if (transferCallback != null) {
            // Player was already disconnected from old downstream
            transferCallback.onTransferFailed();
            return false;
        }

        this.client.close();
        this.player.setPendingConnection(null);
        this.player.sendMessage(new TranslationContainer("waterdog.downstream.transfer.failed", this.client.getServerInfo().getServerName(), packet.getKickMessage()));
        return false;
    }
}
