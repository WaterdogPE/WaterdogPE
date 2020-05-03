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
import com.nukkitx.protocol.bedrock.handler.BedrockPacketHandler;
import com.nukkitx.protocol.bedrock.packet.*;
import com.nukkitx.protocol.bedrock.util.EncryptionUtils;
import pe.waterdog.player.PlayerRewriteData;
import pe.waterdog.player.PlayerRewriteUtils;
import pe.waterdog.player.ProxiedPlayer;

import javax.crypto.SecretKey;
import java.net.URI;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;

public class ConnectedHandler implements BedrockPacketHandler {

    private final ProxiedPlayer player;
    private final ServerInfo serverInfo;
    private final PlayerRewriteData rewrite;

    public ConnectedHandler(ProxiedPlayer player, ServerInfo serverInfo){
        this.player = player;
        this.serverInfo = serverInfo;
        this.rewrite = player.getRewriteData();
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
        player.getPendingConnection().sendPacketImmediately(clientToServerHandshake);
        return true;
    }

    @Override
    public boolean handle(PlayStatusPacket packet) {
        String kickReason = null;
        switch (packet.getStatus()){
            case FAILED_CLIENT:
                kickReason = "outdatedClient";
            break;
            case FAILED_SERVER:
                kickReason = "outdatedServer";
                break;
            case FAILED_EDU_VANILLA:
                kickReason = "serverFull";
                break;
        }

        if (kickReason != null){
            //TODO: Player send message

            if (!this.player.getPendingConnection().isClosed()) this.player.getPendingConnection().disconnect();
            this.player.setPendingConnection(null);
            return true;
        }

        return true;
    }

    @Override
    public boolean handle(ResourcePacksInfoPacket packet) {
        ResourcePackClientResponsePacket response = new ResourcePackClientResponsePacket();
        response.setStatus(ResourcePackClientResponsePacket.Status.HAVE_ALL_PACKS);

        this.player.getPendingConnection().sendPacketImmediately(response);
        return true;
    }

    @Override
    public boolean handle(ResourcePackStackPacket packet) {
        ResourcePackClientResponsePacket response = new ResourcePackClientResponsePacket();
        response.setStatus(ResourcePackClientResponsePacket.Status.COMPLETED);

        this.player.getPendingConnection().sendPacketImmediately(response);
        return true;
    }

    @Override
    public boolean handle(StartGamePacket packet) {
        //Rebridge connections
        this.player.finishTransfer(serverInfo);
        this.player.getRewriteData().setGameRules(packet.getGamerules());
        this.rewrite.setOriginalEntityId(packet.getRuntimeEntityId());


        //PlayerRewriteUtils.injectDimensionChange(this.player.getUpstream(), packet.getDimensionId(), packet.getDefaultSpawn());
        PlayerRewriteUtils.injectChunkPublisherUpdate(this.player.getUpstream(), packet.getDefaultSpawn());
        PlayerRewriteUtils.injectGameMode(this.player.getUpstream(), packet.getPlayerGamemode());

        /*SetLocalPlayerAsInitializedPacket initializedPacket = new SetLocalPlayerAsInitializedPacket();
        initializedPacket.setRuntimeEntityId(PlayerRewriteUtils.rewriteId(packet.getRuntimeEntityId(), rewrite.getEntityId(), rewrite.getOriginalEntityId()));
        this.player.getUpstream().sendPacket(initializedPacket);*/
        return true;
    }
}
