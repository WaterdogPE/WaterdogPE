/*
 * Copyright 2022 WaterdogTEAM
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

package dev.waterdog.waterdogpe.network.protocol.rewrite;

import org.cloudburstmc.protocol.bedrock.data.LevelEventType;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.protocol.bedrock.data.definitions.BlockDefinition;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataMap;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.packet.*;
import dev.waterdog.waterdogpe.network.protocol.rewrite.types.BlockPaletteRewrite;
import dev.waterdog.waterdogpe.network.protocol.rewrite.types.RewriteData;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import io.netty.buffer.AbstractByteBufAllocator;
import io.netty.buffer.ByteBuf;
import org.cloudburstmc.protocol.common.PacketSignal;
import org.cloudburstmc.protocol.common.util.VarInts;

import static org.cloudburstmc.protocol.bedrock.data.LevelEvent.PARTICLE_CRACK_BLOCK;
import static org.cloudburstmc.protocol.bedrock.data.LevelEvent.PARTICLE_DESTROY_BLOCK;
import static org.cloudburstmc.protocol.bedrock.data.ParticleType.TERRAIN;

public class BlockMap implements BedrockPacketHandler {

    protected static final int nV8Blocks = 16 * 16 * 16;

    protected final ProxiedPlayer player;
    protected final RewriteData rewrite;

    public BlockMap(ProxiedPlayer player) {
        this.player = player;
        this.rewrite = player.getRewriteData();
    }

    public BlockPaletteRewrite getPaletteRewrite() {
        return this.rewrite.getBlockPaletteRewrite();
    }

    public PacketSignal doRewrite(BedrockPacket packet) {
        return this.player.canRewrite() ? this.handlePacket(packet) : PacketSignal.UNHANDLED;
    }

    protected int translateId(int runtimeId) {
        return this.getPaletteRewrite().fromDownstream(runtimeId);
    }

    @Override
    public PacketSignal handle(LevelChunkPacket packet) {
        ByteBuf from = packet.getData();
        ByteBuf to = AbstractByteBufAllocator.DEFAULT.ioBuffer(from.readableBytes());

        try {
            boolean success = this.rewriteChunkData(from, to, packet.getSubChunksLength());
            if (success) {
                to.writeBytes(from); // Copy the rest
                packet.setData(to.retain());
            }
            return success ? PacketSignal.HANDLED : PacketSignal.UNHANDLED;
        } finally {
            from.release();
            to.release();
        }
    }

    private boolean rewriteChunkData(ByteBuf from, ByteBuf to, int sections) {
        for (int section = 0; section < sections; section++) {
            int chunkVersion = from.readUnsignedByte();
            to.writeByte(chunkVersion);

            switch (chunkVersion) {
                // Legacy block ids, no remap needed
                // MiNet uses this format
                case 0, 4, 139 -> {
                    to.writeBytes(from);
                    return true;
                }
                case 8 -> { // New form chunk, baked-in palette
                    int storageCount = from.readUnsignedByte();
                    to.writeByte(storageCount);
                    for (int storage = 0; storage < storageCount; storage++) {
                        int flags = from.readUnsignedByte();
                        int bitsPerBlock = flags >> 1; // isRuntime = (flags & 0x1) != 0
                        int blocksPerWord = Integer.SIZE / bitsPerBlock;
                        int nWords = (nV8Blocks + blocksPerWord - 1) / blocksPerWord;

                        to.writeByte(flags);
                        to.writeBytes(from, nWords * Integer.BYTES);

                        int nPaletteEntries = VarInts.readInt(from);
                        VarInts.writeInt(to, nPaletteEntries);

                        for (int i = 0; i < nPaletteEntries; i++) {
                            int runtimeId = VarInts.readInt(from);
                            VarInts.writeInt(to, this.translateId(runtimeId));
                        }
                    }
                }
                default -> { // Unsupported
                    this.player.getLogger().warning("PEBlockRewrite: Unknown subchunk format " + chunkVersion);
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public PacketSignal handle(UpdateBlockPacket packet) {
        int runtimeId = packet.getDefinition().getRuntimeId();
        BlockDefinition definition = this.player.getRewriteData().getCodecHelper()
                .getBlockDefinitions().getDefinition(this.translateId(runtimeId));
        packet.setDefinition(definition);
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(LevelEventPacket packet) {
        LevelEventType type = packet.getType();
        if (type != TERRAIN && type != PARTICLE_DESTROY_BLOCK && type != PARTICLE_CRACK_BLOCK) {
            return PacketSignal.UNHANDLED;
        }
        int data = packet.getData();
        int high = data & 0xFFFF0000;
        int blockID = this.translateId(data & 0xFFFF) & 0xFFFF;
        packet.setData(high | blockID);
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(LevelSoundEventPacket packet) {
        if (packet.getSound() != SoundEvent.PLACE) {
            return PacketSignal.UNHANDLED;
        }

        int runtimeId = packet.getExtraData();
        packet.setExtraData(this.translateId(runtimeId));
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(AddEntityPacket packet) {
        if (!packet.getIdentifier().equals("minecraft:falling_block")) {
            return PacketSignal.UNHANDLED;
        }

        EntityDataMap metaData = packet.getMetadata();
        int runtimeId = metaData.get(EntityDataTypes.VARIANT);
        metaData.put(EntityDataTypes.VARIANT, this.translateId(runtimeId));
        return PacketSignal.HANDLED;
    }
}
