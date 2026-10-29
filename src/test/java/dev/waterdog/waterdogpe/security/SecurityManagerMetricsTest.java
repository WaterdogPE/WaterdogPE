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

package dev.waterdog.waterdogpe.security;

import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.network.NetworkMetrics;
import dev.waterdog.waterdogpe.network.NetworkMetrics.ConnectionError;
import dev.waterdog.waterdogpe.network.NetworkMetrics.LoginFailure;
import dev.waterdog.waterdogpe.network.protocol.user.HandshakeEntry;
import dev.waterdog.waterdogpe.utils.config.proxy.NetworkSettings;
import io.netty.handler.codec.DecoderException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class SecurityManagerMetricsTest {

    private static final SocketAddress CLIENT = new InetSocketAddress("203.0.113.7", 19132);

    private ProxyServer proxy;
    private NetworkMetrics metrics;
    private SecurityManager security;

    @BeforeEach
    public void setUp() {
        this.proxy = mock(ProxyServer.class, RETURNS_DEEP_STUBS);
        // Defaults: 5 sessions and 2 logins per address each second
        when(this.proxy.getNetworkSettings()).thenReturn(new NetworkSettings());
        this.metrics = mock(NetworkMetrics.class);
        when(this.proxy.getNetworkMetrics()).thenReturn(this.metrics);
        this.security = new SecurityManager(this.proxy);
    }

    private static HandshakeEntry entry(boolean xboxAuthed) {
        return new HandshakeEntry(null, null, null, null, "Steve", null, xboxAuthed, null, false);
    }

    @Test
    public void countsThrottledSessions() {
        for (int i = 0; i < 5; i++) {
            assertTrue(this.security.onConnectionCreated(CLIENT));
        }
        verify(this.metrics, never()).loginFailed(any());

        assertFalse(this.security.onConnectionCreated(CLIENT));
        verify(this.metrics).loginFailed(LoginFailure.CONNECTION_THROTTLED);
    }

    @Test
    public void countsThrottledLogins() {
        assertTrue(this.security.onLoginAttempt(CLIENT));
        assertTrue(this.security.onLoginAttempt(CLIENT));
        assertFalse(this.security.onLoginAttempt(CLIENT));
        verify(this.metrics).loginFailed(LoginFailure.LOGIN_THROTTLED);
    }

    @Test
    public void countsAListenerRefusalOnlyWhenTheThrottleLetItThrough() {
        this.security.setListener(new SecurityListener() {
            @Override
            public boolean onLoginAttempt(SocketAddress address) {
                return false;
            }
        });

        assertFalse(this.security.onLoginAttempt(CLIENT));
        assertFalse(this.security.onLoginAttempt(CLIENT));
        assertFalse(this.security.onLoginAttempt(CLIENT));
        verify(this.metrics, times(2)).loginFailed(LoginFailure.LISTENER);
        verify(this.metrics).loginFailed(LoginFailure.LOGIN_THROTTLED);
    }

    @Test
    public void tellsAnUnsignedChainFromAKeyMismatch() {
        when(this.proxy.getConfiguration().isOnlineMode()).thenReturn(true);

        this.security.onLoginFailed(CLIENT, entry(false), null, "disconnectionScreen.notAuthenticated");
        verify(this.metrics).loginFailed(LoginFailure.XBOX_AUTH);

        this.security.onLoginFailed(CLIENT, entry(true), null, "disconnectionScreen.notAuthenticated");
        verify(this.metrics).loginFailed(LoginFailure.IDENTITY_KEY);
    }

    @Test
    public void readsAnUnsignedChainAsAKeyMismatchInOfflineMode() {
        when(this.proxy.getConfiguration().isOnlineMode()).thenReturn(false);

        this.security.onLoginFailed(CLIENT, entry(false), null, "disconnectionScreen.notAuthenticated");
        verify(this.metrics).loginFailed(LoginFailure.IDENTITY_KEY);
    }

    @Test
    public void countsALoginThatThrewAsAnError() {
        this.security.onLoginFailed(CLIENT, entry(true), new IllegalStateException("bad chain"), "Login failed");
        this.security.onLoginFailed(CLIENT, null, new IllegalStateException("bad chain"), "Login failed");
        verify(this.metrics, times(2)).loginFailed(LoginFailure.ERROR);
    }

    @Test
    public void namesWhatClosedAConnection() {
        this.security.onLoginTimeout(CLIENT);
        verify(this.metrics).connectionError(ConnectionError.TIMEOUT);

        this.security.onConnectionError(CLIENT, null);
        verify(this.metrics).connectionError(ConnectionError.PROTOCOL);

        this.security.onConnectionError(CLIENT, new DecoderException("Batch holds an empty packet"));
        verify(this.metrics).connectionError(ConnectionError.DECODE);

        this.security.onConnectionError(CLIENT, new IOException("Connection reset"));
        verify(this.metrics).connectionError(ConnectionError.IO);

        this.security.onConnectionError(CLIENT, new NullPointerException());
        verify(this.metrics).connectionError(ConnectionError.OTHER);
    }

    @Test
    public void reportsNothingWithoutMetrics() {
        when(this.proxy.getNetworkMetrics()).thenReturn(null);

        for (int i = 0; i < 6; i++) {
            this.security.onConnectionCreated(CLIENT);
        }
        this.security.onLoginFailed(CLIENT, entry(false), null, "disconnectionScreen.notAuthenticated");
        this.security.onLoginTimeout(CLIENT);
        verifyNoInteractions(this.metrics);
    }
}
