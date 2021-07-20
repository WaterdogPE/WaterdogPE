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

package dev.waterdog.waterdogpe.network.session;

import com.nukkitx.protocol.bedrock.BedrockPacket;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import io.netty.buffer.ByteBuf;

import javax.crypto.SecretKey;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.function.Consumer;

public interface DownstreamSession {

    void onDownstreamInit(ProxiedPlayer player, boolean initial);
    void onInitialServerConnected(ProxiedPlayer player);
    void onServerConnected(ProxiedPlayer player);
    void onTransferCompleted(ProxiedPlayer player, Runnable completedCallback);

    void addDisconnectHandler(Consumer<Object> handler);

    void sendPacket(BedrockPacket packet);
    void sendPacketImmediately(BedrockPacket packet);

    void sendWrapped(Collection<BedrockPacket> packets, boolean encrypt);
    void sendWrapped(ByteBuf compressed, boolean encrypt);

    default void enableEncryption(SecretKey secretKey) {
        // Implement if supported
    }

    default boolean isEncrypted() {
        return false;
    }

    int getHardcodedBlockingId();

    InetSocketAddress getAddress();
    long getLatency();

    boolean isClosed();

    void disconnect();

}
