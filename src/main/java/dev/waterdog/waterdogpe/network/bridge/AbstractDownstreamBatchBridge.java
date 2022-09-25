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

package dev.waterdog.waterdogpe.network.bridge;

import com.nukkitx.protocol.bedrock.BedrockPacket;
import com.nukkitx.protocol.bedrock.BedrockSession;
import dev.waterdog.waterdogpe.network.session.CompressionAlgorithm;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import io.netty.buffer.ByteBuf;

import java.util.Collection;

/**
 * This is the default implementation of downstream to upstream BatchBridge which holds a reference to the upstream session.
 */
public abstract class AbstractDownstreamBatchBridge extends ProxyBatchBridge {

    protected final BedrockSession upstreamSession;

    public AbstractDownstreamBatchBridge(ProxiedPlayer player, BedrockSession upstreamSession) {
        super(player);
        this.upstreamSession = upstreamSession;
    }

    @Override
    public void sendWrapped(Collection<BedrockPacket> packets, boolean encrypt) {
        this.upstreamSession.sendWrapped(packets, encrypt);
    }

    @Override
    public void sendWrapped(ByteBuf compressed, boolean encrypt) {
        this.upstreamSession.sendWrapped(compressed, encrypt);
    }

    @Override
    public CompressionAlgorithm getCompression() {
        return this.player.getUpstreamCompression();
    }

    @Override
    public boolean isEncrypted() {
        return this.upstreamSession.isEncrypted();
    }
}
