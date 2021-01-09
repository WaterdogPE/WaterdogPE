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

package pe.waterdog.network.session;

import com.nimbusds.jose.JWSObject;
import com.nukkitx.protocol.bedrock.packet.LoginPacket;
import io.netty.util.AsciiString;
import lombok.Builder;
import pe.waterdog.network.protocol.ProtocolVersion;

import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.util.UUID;

/**
 * Holds relevant information passed to the proxy on the first connection (initial) in the LoginPacket.
 */
@Builder
public class LoginData {

    private final String displayName;
    private final UUID uuid;
    private final String xuid;
    private final boolean xboxAuthed;
    private final InetSocketAddress address;
    private final ProtocolVersion protocol;
    private final String joinHostname;

    private final KeyPair keyPair;
    private final AsciiString chainData;
    private final JWSObject signedClientData;

    public LoginPacket constructLoginPacket() {
        LoginPacket loginPacket = new LoginPacket();
        loginPacket.setChainData(this.chainData);
        loginPacket.setSkinData(AsciiString.of(this.signedClientData.serialize()));
        loginPacket.setProtocolVersion(this.protocol.getProtocol());
        return loginPacket;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public String getXuid() {
        return this.xuid;
    }

    public boolean isXboxAuthed() {
        return this.xboxAuthed;
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public InetSocketAddress getAddress() {
        return this.address;
    }

    public ProtocolVersion getProtocol() {
        return this.protocol;
    }

    public KeyPair getKeyPair() {
        return this.keyPair;
    }

    public String getJoinHostname() {
        return this.joinHostname;
    }

    public JWSObject getSignedClientData() {
        return this.signedClientData;
    }
}
