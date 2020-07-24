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
import com.nukkitx.protocol.bedrock.BedrockClientSession;
import com.nukkitx.protocol.bedrock.handler.BedrockPacketHandler;
import com.nukkitx.protocol.bedrock.packet.*;
import com.nukkitx.protocol.bedrock.util.EncryptionUtils;
import lombok.SneakyThrows;
import pe.waterdog.network.ServerInfo;
import pe.waterdog.network.bridge.ProxyBatchBridge;
import pe.waterdog.network.session.RewriteData;
import pe.waterdog.player.PlayerRewriteUtils;
import pe.waterdog.player.ProxiedPlayer;
import pe.waterdog.utils.exceptions.CancelSignalException;

import javax.crypto.SecretKey;
import java.net.URI;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;

public class ConnectedHandler implements BedrockPacketHandler {

    private final ProxiedPlayer player;
    private final BedrockClientSession server;
    private final ServerInfo serverInfo;
    private final RewriteData rewrite;

    public ConnectedHandler(ProxiedPlayer player, ServerInfo serverInfo, BedrockClientSession session){
        this.player = player;
        this.serverInfo = serverInfo;
        this.rewrite = player.getRewriteData();
        this.server = session;
    }

    @Override
    public boolean handle(ServerToClientHandshakePacket packet) {
        try {
            SignedJWT saltJwt = SignedJWT.parse(packet.getJwt());
            URI x5u = saltJwt.getHeader().getX509CertURL();
            ECPublicKey serverKey = EncryptionUtils.generateKey(x5u.toASCIIString());
            SecretKey key = EncryptionUtils.getSecretKey(player.getKeyPair().getPrivate(), serverKey,
                    Base64.getDecoder().decode(saltJwt.getJWTClaimsSet().getStringClaim("salt")));
            player.getDownstream().enableEncryption(key);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        ClientToServerHandshakePacket clientToServerHandshake = new ClientToServerHandshakePacket();
        this.server.sendPacketImmediately(clientToServerHandshake);
        return true;
    }

    @SneakyThrows
    @Override
    public boolean handle(PlayStatusPacket packet) {
        String kickReason = null;
        /*switch (packet.getStatus()){
            case FAILED_CLIENT:
                kickReason = "outdatedClient";
            break;
            case FAILED_SERVER:
                kickReason = "outdatedServer";
                break;
            case FAILED_SERVER_FULL:
                kickReason = "serverFull";
                break;
            case FAILED_EDU_VANILLA:
            case FAILED_VANILLA_EDU:
            case FAILED_INVALID_TENANT:
                break;
            case PLAYER_SPAWN:
                //if (this.player.isDimensionChange()) throw CancelSignalException.CANCEL;
            default:
                return true;
        }*/

        //TODO: Player send message

        if (!this.server.isClosed()) this.server.disconnect();
        this.player.getPendingConnections().remove();
        return true;
    }

    @Override
    public boolean handle(ResourcePacksInfoPacket packet) {
        ResourcePackClientResponsePacket response = new ResourcePackClientResponsePacket();
        response.setStatus(ResourcePackClientResponsePacket.Status.HAVE_ALL_PACKS);
        this.server.sendPacket(response);
        return true;
    }

    @Override
    public boolean handle(ResourcePackStackPacket packet) {
        ResourcePackClientResponsePacket response = new ResourcePackClientResponsePacket();
        response.setStatus(ResourcePackClientResponsePacket.Status.COMPLETED);
        this.server.sendPacket(response);
        return true;
    }

    @SneakyThrows
    @Override
    public boolean handle(StartGamePacket packet) {
        this.player.getRewriteData().setGameRules(packet.getGamerules());
        this.rewrite.setOriginalEntityId(packet.getRuntimeEntityId());

        //send DIM ID 1 & than original dim
        if (this.rewrite.getDimension() == packet.getDimensionId()){
            this.player.setDimensionChange(true);
            PlayerRewriteUtils.injectDimensionChange(this.player.getUpstream(), packet.getDimensionId() == 0 ? 1 : 0);
            //PlayerRewriteUtils.injectStatusChange(this.player.getUpstream(), PlayStatusPacket.Status.PLAYER_SPAWN);
        }

        this.rewrite.setDimension(packet.getDimensionId());

        PlayerRewriteUtils.injectChunkPublisherUpdate(this.player.getUpstream(), packet.getDefaultSpawn());
        PlayerRewriteUtils.injectGameMode(this.player.getUpstream(), packet.getLevelGameType());

        SetLocalPlayerAsInitializedPacket initializedPacket = new SetLocalPlayerAsInitializedPacket();
        initializedPacket.setRuntimeEntityId(PlayerRewriteUtils.rewriteId(packet.getRuntimeEntityId(),
                player.getRewriteData().getEntityId(),
                player.getRewriteData().getOriginalEntityId()));
        this.server.sendPacket(initializedPacket);

        this.player.finishTransfer(serverInfo);
        return true;
    }
}
