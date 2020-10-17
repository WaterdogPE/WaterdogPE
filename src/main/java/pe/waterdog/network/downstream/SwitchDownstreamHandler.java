/**
 * Copyright 2020 WaterdogTEAM
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pe.waterdog.network.downstream;

import com.nimbusds.jwt.SignedJWT;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.BedrockClient;
import com.nukkitx.protocol.bedrock.BedrockClientSession;
import com.nukkitx.protocol.bedrock.handler.BedrockPacketHandler;
import com.nukkitx.protocol.bedrock.packet.*;
import com.nukkitx.protocol.bedrock.util.EncryptionUtils;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import pe.waterdog.ProxyServer;
import pe.waterdog.event.defaults.TransferCompleteEvent;
import pe.waterdog.network.ServerInfo;
import pe.waterdog.network.rewrite.types.BlockPalette;
import pe.waterdog.network.rewrite.types.RewriteData;
import pe.waterdog.network.session.ServerConnection;
import pe.waterdog.network.session.SessionInjections;
import pe.waterdog.player.PlayerRewriteUtils;
import pe.waterdog.player.ProxiedPlayer;
import pe.waterdog.utils.exceptions.CancelSignalException;

import javax.crypto.SecretKey;
import java.net.URI;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;
import java.util.Collection;
import java.util.UUID;

public class SwitchDownstreamHandler implements BedrockPacketHandler {

    private final ProxiedPlayer player;
    private final BedrockClient client;
    private final ServerInfo serverInfo;

    public SwitchDownstreamHandler(ProxiedPlayer player, ServerInfo serverInfo, BedrockClient client) {
        this.player = player;
        this.serverInfo = serverInfo;
        this.client = client;
    }

    public BedrockClientSession getDownstream() {
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
        switch (packet.getStatus()){
            case LOGIN_SUCCESS:
                throw CancelSignalException.CANCEL;
            case LOGIN_FAILED_CLIENT_OLD:
            case LOGIN_FAILED_SERVER_OLD:
            case FAILED_SERVER_FULL_SUB_CLIENT:
                //TODO: handle error
                throw CancelSignalException.CANCEL;
        }
        return false;
    }

    @Override
    public final boolean handle(StartGamePacket packet) {
        RewriteData rewriteData = player.getRewriteData();
        rewriteData.setOriginalEntityId(packet.getRuntimeEntityId());
        rewriteData.setDimension(packet.getDimensionId());
        rewriteData.setGameRules(packet.getGamerules());
        rewriteData.setSpawnPosition(packet.getPlayerPosition());
        rewriteData.setRotation(packet.getRotation());

        BlockPalette palette = BlockPalette.getPalette(packet.getBlockPalette(), this.player.getProtocol());
        rewriteData.setPaletteRewrite(palette.createRewrite(rewriteData.getBlockPalette()));
        long runtimeId = PlayerRewriteUtils.rewriteId(packet.getRuntimeEntityId(), rewriteData.getEntityId(), rewriteData.getOriginalEntityId());

        PlayerRewriteUtils.injectChunkPublisherUpdate(this.player.getUpstream(), packet.getDefaultSpawn());
        PlayerRewriteUtils.injectGameMode(this.player.getUpstream(), packet.getPlayerGameType());

        SetLocalPlayerAsInitializedPacket initializedPacket = new SetLocalPlayerAsInitializedPacket();
        initializedPacket.setRuntimeEntityId(runtimeId);
        this.getDownstream().sendPacket(initializedPacket);

        MovePlayerPacket movePlayerPacket = new MovePlayerPacket();
        movePlayerPacket.setPosition(packet.getPlayerPosition());
        movePlayerPacket.setRuntimeEntityId(runtimeId);
        movePlayerPacket.setRotation(Vector3f.from(packet.getRotation().getX(), packet.getRotation().getY(), packet.getRotation().getY()));
        movePlayerPacket.setMode(MovePlayerPacket.Mode.RESPAWN);
        this.player.sendPacket(movePlayerPacket);

        this.getDownstream().sendPacket(rewriteData.getChunkRadius());
        PlayerRewriteUtils.injectRemoveAllEffects(this.player.getUpstream(), rewriteData.getEntityId());
        PlayerRewriteUtils.injectClearWeather(this.player.getUpstream());

        Collection<UUID> playerList = this.player.getPlayers();
        PlayerRewriteUtils.injectRemoveAllPlayers(this.player.getUpstream(), playerList);
        playerList.clear();

        LongSet entities = this.player.getEntities();
        for (long entityId : entities) {
            PlayerRewriteUtils.injectRemoveEntity(this.player.getUpstream(), entityId);
        }
        entities.clear();

        ObjectSet<String> scoreboards = this.player.getScoreboards();
        for (String scoreboard : scoreboards) {
            PlayerRewriteUtils.injectRemoveObjective(this.player.getUpstream(), scoreboard);
        }
        scoreboards.clear();

        LongSet bossbars = this.player.getBossbars();
        for (long bossbarId : bossbars){
            PlayerRewriteUtils.injectRemoveBossbar(this.player.getUpstream(), bossbarId);
        }
        bossbars.clear();

        PlayerRewriteUtils.injectGameRules(this.player.getUpstream(), rewriteData.getGameRules());
        PlayerRewriteUtils.injectSetDifficulty(this.player.getUpstream(), packet.getDifficulty());

        /*//send DIM ID 1 & than original dim
        if (this.rewrite.getDimension() == packet.getDimensionId()){
            this.player.setDimensionChange(true);
            PlayerRewriteUtils.injectDimensionChange(this.player.getUpstream(), packet.getDimensionId() == 0 ? 1 : 0);
            //PlayerRewriteUtils.injectStatusChange(this.player.getUpstream(), PlayStatusPacket.Status.PLAYER_SPAWN);
        }*/
        //this.player.transferServer(serverInfo);

        ServerConnection oldServer = this.player.getServer();
        oldServer.getInfo().removePlayer(this.player);
        oldServer.disconnect();

        this.serverInfo.addPlayer(this.player);
        this.player.setPendingConnection(null);

        ServerConnection server = new ServerConnection(this.client, this.getDownstream(), this.serverInfo);
        SessionInjections.injectDownstreamHandlers(server, this.player);
        this.player.setServer(server);
        this.player.setAcceptPlayStatus(true);

        TransferCompleteEvent event = new TransferCompleteEvent(oldServer, server, this.player);
        ProxyServer.getInstance().getEventManager().callEvent(event);
        throw CancelSignalException.CANCEL;
    }
}
