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
import com.nukkitx.protocol.bedrock.packet.ClientToServerHandshakePacket;
import com.nukkitx.protocol.bedrock.packet.PlayStatusPacket;
import com.nukkitx.protocol.bedrock.packet.ServerToClientHandshakePacket;
import com.nukkitx.protocol.bedrock.packet.StartGamePacket;
import com.nukkitx.protocol.bedrock.util.EncryptionUtils;
import lombok.SneakyThrows;
import pe.waterdog.network.upstream.UpstreamHandler;
import pe.waterdog.player.PlayerRewriteUtils;
import pe.waterdog.player.ProxiedPlayer;
import pe.waterdog.player.PlayerRewriteData;
import pe.waterdog.utils.exceptions.CancelSignalException;

import javax.crypto.SecretKey;
import java.net.URI;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;
import java.util.concurrent.ThreadLocalRandom;

public class InitialHandler implements BedrockPacketHandler {

    private final ProxiedPlayer player;

    public InitialHandler(ProxiedPlayer player){
        this.player = player;
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
        player.getDownstream().sendPacketImmediately(clientToServerHandshake);
        return true;
    }

    @SneakyThrows
    @Override
    public boolean handle(StartGamePacket packet) {
        PlayerRewriteData rewrite = new PlayerRewriteData(
                ThreadLocalRandom.current().nextInt(10000, 15000),
                packet.getRuntimeEntityId(),
                packet.getBlockPalette(),
                packet.getGamerules()
        );

        packet.setRuntimeEntityId(rewrite.getEntityId());
        packet.setUniqueEntityId(rewrite.getEntityId());

        this.player.setRewriteData(rewrite);
        this.player.getEntityMap().setRewriteData(rewrite);
        this.player.getUpstream().setPacketHandler(new UpstreamHandler(this.player, this.player.getUpstream()));
        return false;
    }

    public boolean handle(PlayStatusPacket packet) {
        if (packet.getStatus() == PlayStatusPacket.Status.PLAYER_SPAWN){
            this.player.getDownstream().setPacketHandler(new ConnectedHandler(this.player, this.player.getServerInfo()));
        }

        return false;
    }
}
