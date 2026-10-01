/*
 * Copyright 2026 WaterdogTEAM
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

package dev.waterdog.waterdogpe.network.connection.client;

import dev.waterdog.waterdogpe.network.protocol.ProtocolVersion;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoop;
import org.cloudburstmc.netty.channel.raknet.RakChannel;
import org.cloudburstmc.netty.handler.codec.raknet.common.RakSessionCodec;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BedrockClientConnectionTest {

    private final RakChannel channel = mock(RakChannel.class);
    private final RakSessionCodec sessionCodec = mock(RakSessionCodec.class);

    private BedrockClientConnection connection(boolean open) {
        EventLoop eventLoop = mock(EventLoop.class);
        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return null;
        }).when(eventLoop).execute(any(Runnable.class));

        ChannelPipeline rakPipeline = mock(ChannelPipeline.class);
        when(rakPipeline.get(RakSessionCodec.class)).thenReturn(this.sessionCodec);

        when(this.channel.eventLoop()).thenReturn(eventLoop);
        when(this.channel.rakPipeline()).thenReturn(rakPipeline);
        when(this.channel.isOpen()).thenReturn(open);

        ProxiedPlayer player = mock(ProxiedPlayer.class);
        when(player.getProtocol()).thenReturn(ProtocolVersion.latest());
        return new BedrockClientConnection(player, mock(ServerInfo.class), this.channel);
    }

    @Test
    void disconnectEndsTheRakNetSessionInsteadOfDisconnectingTheSocket() {
        this.connection(true).disconnect();

        verify(this.sessionCodec).disconnect();
        verify(this.channel, never()).disconnect();
    }

    @Test
    void disconnectOnClosedChannelTouchesNothing() {
        // The socket behind a closed channel may already have handed its fd to another connection
        this.connection(false).disconnect();

        verify(this.sessionCodec, never()).disconnect();
        verify(this.channel, never()).disconnect();
        verify(this.channel, never()).close();
    }
}
