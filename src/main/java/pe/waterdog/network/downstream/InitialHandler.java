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
import com.nukkitx.protocol.bedrock.packet.ServerToClientHandshakePacket;
import com.nukkitx.protocol.bedrock.packet.StartGamePacket;
import com.nukkitx.protocol.bedrock.util.EncryptionUtils;
import pe.waterdog.network.rewrite.types.BlockPalette;
import pe.waterdog.network.rewrite.types.RewriteData;
import pe.waterdog.player.ProxiedPlayer;
import pe.waterdog.utils.exceptions.CancelSignalException;

import javax.crypto.SecretKey;
import java.net.URI;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;
import java.util.concurrent.ThreadLocalRandom;

public class InitialHandler implements BedrockPacketHandler {

    private final ProxiedPlayer player;

    public InitialHandler(ProxiedPlayer player) {
        this.player = player;
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
            this.player.getServer().getDownstream().enableEncryption(key);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        ClientToServerHandshakePacket clientToServerHandshake = new ClientToServerHandshakePacket();
        this.player.getServer().sendPacket(clientToServerHandshake);
        throw CancelSignalException.CANCEL;
    }

    @Override
    public final boolean handle(StartGamePacket packet) {
        RewriteData rewrite = this.player.getRewriteData();
        rewrite.setOriginalEntityId(packet.getRuntimeEntityId());
        rewrite.setEntityId(ThreadLocalRandom.current().nextInt(10000, 15000));
        rewrite.setGameRules(packet.getGamerules());
        rewrite.setDimension(packet.getDimensionId());

        BlockPalette palette = BlockPalette.getPalette(packet.getBlockPalette(), this.player.getProtocol());
        rewrite.setBlockPalette(palette);
        rewrite.setPaletteRewrite(palette.createRewrite(palette));

        this.player.setCanRewrite(true);

        packet.setRuntimeEntityId(rewrite.getEntityId());
        packet.setUniqueEntityId(rewrite.getEntityId());

        this.player.getServer().getDownstream().setPacketHandler(new ConnectedDownstreamHandler(this.player, this.player.getServer()));
        return true;
    }
}
