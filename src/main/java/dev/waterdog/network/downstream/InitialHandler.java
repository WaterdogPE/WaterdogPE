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

package dev.waterdog.network.downstream;

import com.nimbusds.jwt.SignedJWT;
import com.nukkitx.protocol.bedrock.packet.*;
import com.nukkitx.protocol.bedrock.util.EncryptionUtils;
import dev.waterdog.network.protocol.ProtocolVersion;
import dev.waterdog.network.rewrite.BlockMap;
import dev.waterdog.network.rewrite.BlockMapModded;
import dev.waterdog.network.rewrite.types.BlockPalette;
import dev.waterdog.network.rewrite.types.RewriteData;
import dev.waterdog.network.session.SessionInjections;
import dev.waterdog.utils.exceptions.CancelSignalException;
import dev.waterdog.player.ProxiedPlayer;

import javax.crypto.SecretKey;
import java.net.URI;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;
import java.util.concurrent.ThreadLocalRandom;

public class InitialHandler extends AbstractDownstreamHandler {

    public InitialHandler(ProxiedPlayer player) {
        super(player);
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
    public final boolean handle(ResourcePacksInfoPacket packet) {
        if (!this.player.getProxy().getConfiguration().enabledResourcePacks() || !this.player.acceptResourcePacks()) {
            return false;
        }
        ResourcePackClientResponsePacket response = new ResourcePackClientResponsePacket();
        response.setStatus(ResourcePackClientResponsePacket.Status.HAVE_ALL_PACKS);
        this.player.getServer().getDownstream().sendPacketImmediately(response);
        throw CancelSignalException.CANCEL;
    }

    @Override
    public final boolean handle(ResourcePackStackPacket packet) {
        if (!this.player.getProxy().getConfiguration().enabledResourcePacks() || !this.player.acceptResourcePacks()) {
            return false;
        }
        ResourcePackClientResponsePacket response = new ResourcePackClientResponsePacket();
        response.setStatus(ResourcePackClientResponsePacket.Status.COMPLETED);
        this.player.getServer().getDownstream().sendPacketImmediately(response);
        throw CancelSignalException.CANCEL;
    }

    @Override
    public final boolean handle(StartGamePacket packet) {
        RewriteData rewriteData = this.player.getRewriteData();
        rewriteData.setOriginalEntityId(packet.getRuntimeEntityId());
        rewriteData.setEntityId(ThreadLocalRandom.current().nextInt(10000, 15000));
        rewriteData.setGameRules(packet.getGamerules());
        rewriteData.setDimension(packet.getDimensionId());
        rewriteData.parseItemIds(packet.getItemEntries());
        rewriteData.setSpawnPosition(packet.getPlayerPosition());

        // Starting with 419 server does not send vanilla blocks to client
        if (this.player.getProtocol().getProtocol() <= ProtocolVersion.MINECRAFT_PE_1_16_20.getProtocol()){
            BlockPalette palette = BlockPalette.getPalette(packet.getBlockPalette(), this.player.getProtocol());
            rewriteData.setBlockPalette(palette);
            rewriteData.setBlockPaletteRewrite(palette.createRewrite(palette));
            this.player.getRewriteMaps().setBlockMap(new BlockMap(this.player));
        }else {
            rewriteData.setBlockProperties(packet.getBlockProperties());
            this.player.getRewriteMaps().setBlockMap(new BlockMapModded(this.player));
        }

        this.player.setCanRewrite(true);

        packet.setRuntimeEntityId(rewriteData.getEntityId());
        packet.setUniqueEntityId(rewriteData.getEntityId());

        SessionInjections.injectInitialHandlers(this.player.getServer(), this.player);
        return true;
    }
}
