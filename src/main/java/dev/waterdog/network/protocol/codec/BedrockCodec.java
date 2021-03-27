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

package dev.waterdog.network.protocol.codec;

import com.google.common.base.Preconditions;
import com.nukkitx.protocol.bedrock.BedrockPacketCodec;
import dev.waterdog.ProxyServer;
import dev.waterdog.event.defaults.ProtocolCodecRegisterEvent;
import dev.waterdog.network.protocol.ProtocolVersion;

public abstract class BedrockCodec {

    private BedrockPacketCodec packetCodec;

    public BedrockCodec() {
    }

    public BedrockPacketCodec.Builder createBuilder(BedrockPacketCodec defaultCodec) {
        return this.createBuilder(defaultCodec.getProtocolVersion(), defaultCodec.getRaknetProtocolVersion(), defaultCodec.getMinecraftVersion());
    }

    /**
     * Creates default builder that will be used in buildCodec() method.
     *
     * @param protocol      protocol number.
     * @param raknetVersion version number of RakNet that client uses.
     * @param minecraftVer  name of version in string.
     * @return BedrockPacketCodec builder.
     */
    public BedrockPacketCodec.Builder createBuilder(int protocol, int raknetVersion, String minecraftVer) {
        Preconditions.checkArgument(this.packetCodec == null, "Packet codec has been already built!");
        BedrockPacketCodec.Builder builder = BedrockPacketCodec.builder();
        builder.protocolVersion(protocol);
        builder.raknetProtocolVersion(raknetVersion);
        builder.minecraftVersion(minecraftVer);
        return builder;
    }

    /**
     * Used to fully initialize BedrockPacketCodec and register all packets which are going to be used by proxy.
     *
     * @param protocol ProtocolVersion instance which will be used to determine default codec. Should be same as one returned in getProtocol().
     * @param proxy    instance of ProxyServer.
     * @return if codec was successfully initialized and can be registered.
     */
    public final boolean initializeCodec(ProtocolVersion protocol, ProxyServer proxy) {
        BedrockPacketCodec.Builder builder = this.createBuilder(protocol.getDefaultCodec());
        this.buildCodec(builder);

        if (proxy.getConfiguration().injectCommands()) {
            this.registerCommands(builder);
        }

        // We use ProtocolCodecRegisterEvent to modify final codec.
        // When this event is canceled codec will not be registered.
        ProtocolCodecRegisterEvent event = new ProtocolCodecRegisterEvent(protocol, builder);
        proxy.getEventManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }

        this.setPacketCodec(builder.build());
        return true;
    }

    /**
     * This method should be implemented in parent.
     * Some common packets may be implemented here later.
     *
     * @param builder can be edited inside of the function. Builder is used to register or deregister packets.
     */
    protected void buildCodec(BedrockPacketCodec.Builder builder) {
        // Maybe later put common packets here
    }

    public void registerCommands(BedrockPacketCodec.Builder builder) {
        // Used to register packets related command injections
    }

    public abstract ProtocolVersion getProtocol();

    public BedrockPacketCodec getPacketCodec() {
        return this.packetCodec;
    }

    public void setPacketCodec(BedrockPacketCodec packetCodec) {
        Preconditions.checkNotNull(packetCodec, "New packet codec can not be null!");
        Preconditions.checkArgument(this.packetCodec == null, "Packet Codec cannot be overwritten on Runtime!");
        this.packetCodec = packetCodec;
    }
}
