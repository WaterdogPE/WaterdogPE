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

package dev.waterdog.network.rewrite;

import com.nukkitx.network.VarInts;
import com.nukkitx.protocol.bedrock.BedrockPacket;
import com.nukkitx.protocol.bedrock.data.LevelEventType;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.data.entity.EntityDataMap;
import com.nukkitx.protocol.bedrock.handler.BedrockPacketHandler;
import com.nukkitx.protocol.bedrock.packet.*;
import dev.waterdog.network.rewrite.types.BlockPaletteRewrite;
import dev.waterdog.network.rewrite.types.RewriteData;
import dev.waterdog.player.ProxiedPlayer;
import io.netty.buffer.AbstractByteBufAllocator;
import io.netty.buffer.ByteBuf;

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

    public boolean doRewrite(BedrockPacket packet) {
        return this.player.canRewrite() && packet.handle(this);
    }

    protected int translateId(int runtimeId) {
        return this.getPaletteRewrite().fromDownstream(runtimeId);
    }

    @Override
    public boolean handle(LevelChunkPacket packet) {
        byte[] oldData = packet.getData();
        ByteBuf from = AbstractByteBufAllocator.DEFAULT.ioBuffer(oldData.length);
        ByteBuf to = AbstractByteBufAllocator.DEFAULT.ioBuffer(oldData.length);

        try {
            from.writeBytes(oldData);
            boolean success = this.rewriteChunkData(from, to, packet.getSubChunksLength());
            if (success) {
                to.writeBytes(from); // Copy the rest
                byte[] newData = new byte[to.readableBytes()];
                to.readBytes(newData);
                packet.setData(newData);
            }
            return success;
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
                case 0: // Legacy block ids, no remap needed
                case 4: // MiNet uses this format
                case 139:
                    to.writeBytes(from);
                    return true;
                case 8: // New form chunk, baked-in palette
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
                    break;
                default: // Unsupported
                    this.player.getLogger().warning("PEBlockRewrite: Unknown subchunk format " + chunkVersion);
                    return false;
            }
        }
        return true;
    }

    @Override
    public boolean handle(UpdateBlockPacket packet) {
        int runtimeId = packet.getRuntimeId();
        packet.setRuntimeId(this.translateId(runtimeId));
        return true;
    }

    @Override
    public boolean handle(LevelEventPacket packet) {
        LevelEventType type = packet.getType();
        switch (type) {
            case PARTICLE_TERRAIN:
            case PARTICLE_DESTROY_BLOCK:
            case PARTICLE_CRACK_BLOCK:
                break;
            default:
                return false;
        }

        int data = packet.getData();
        int high = data & 0xFFFF0000;
        int blockID = this.translateId(data & 0xFFFF) & 0xFFFF;
        packet.setData(high | blockID);
        return true;
    }

    @Override
    public boolean handle(LevelSoundEventPacket packet) {
        if (packet.getSound() != SoundEvent.PLACE) {
            return false;
        }

        int runtimeId = packet.getExtraData();
        packet.setExtraData(this.translateId(runtimeId));
        return true;
    }

    @Override
    public boolean handle(AddEntityPacket packet) {
        if (!packet.getIdentifier().equals("minecraft:falling_block")) {
            return false;
        }

        EntityDataMap metaData = packet.getMetadata();
        int runtimeId = metaData.getInt(EntityData.VARIANT);
        metaData.putInt(EntityData.VARIANT, this.translateId(runtimeId));
        return true;
    }
}
