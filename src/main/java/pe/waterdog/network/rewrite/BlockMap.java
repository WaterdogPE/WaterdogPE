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

package pe.waterdog.network.rewrite;

import com.nukkitx.network.VarInts;
import com.nukkitx.protocol.bedrock.BedrockPacket;
import com.nukkitx.protocol.bedrock.data.LevelEventType;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.data.entity.EntityDataMap;
import com.nukkitx.protocol.bedrock.handler.BedrockPacketHandler;
import com.nukkitx.protocol.bedrock.packet.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import pe.waterdog.network.rewrite.types.BlockPaletteRewrite;
import pe.waterdog.network.rewrite.types.RewriteData;
import pe.waterdog.player.ProxiedPlayer;

public class BlockMap implements BedrockPacketHandler {

    private static final int nV8Blocks = 16 * 16 * 16;

    private final ProxiedPlayer player;
    private final RewriteData rewrite;

    public BlockMap(ProxiedPlayer player) {
        this.player = player;
        this.rewrite = player.getRewriteData();
    }

    public BlockPaletteRewrite getPaletteRewrite() {
        return this.rewrite.getPaletteRewrite();
    }

    public boolean doRewrite(BedrockPacket packet) {
        return this.player.canRewrite() && packet.handle(this);
    }

    @Override
    public boolean handle(LevelChunkPacket packet) {
        int sections = packet.getSubChunksLength();
        byte[] oldData = packet.getData();
        ByteBuf from = PooledByteBufAllocator.DEFAULT.directBuffer(oldData.length);
        ByteBuf to = PooledByteBufAllocator.DEFAULT.directBuffer(oldData.length);
        from.writeBytes(oldData);

        boolean success = true;
        for (int section = 0; section < sections; section++) {
            boolean notSupported = false;
            int chunkVersion = from.readUnsignedByte();
            to.writeByte(chunkVersion);

            switch (chunkVersion) {
                case 0: //legacy block ids, no remap needed
                case 4: //minet uses this format. what is it?
                case 139:
                    from.release();
                    to.release();
                    return false;
                case 8: //new form chunk, baked-in palette
                    int storageCount = from.readUnsignedByte();
                    to.writeByte(storageCount);

                    for (int storage = 0; storage < storageCount; storage++) {
                        int flags = from.readUnsignedByte();
                        int bitsPerBlock = flags >> 1; //isRuntime = (flags & 0x1) != 0
                        int blocksPerWord = Integer.SIZE / bitsPerBlock;
                        int nWords = (nV8Blocks + blocksPerWord - 1) / blocksPerWord;

                        to.writeByte(flags);
                        to.writeBytes(from, nWords * Integer.BYTES);

                        int nPaletteEntries = VarInts.readInt(from);
                        VarInts.writeInt(to, nPaletteEntries);

                        for (int i = 0; i < nPaletteEntries; i++) {
                            int runtimeId = VarInts.readInt(from);
                            VarInts.writeInt(to, this.getPaletteRewrite().map(runtimeId));
                        }
                    }
                    break;
                default: //unsupported
                    notSupported = true;
                    this.player.getLogger().warning("PEBlockRewrite: Unknown subchunk format " + chunkVersion);
                    break;
            }

            if (notSupported) {
                success = false;
                break;
            }
        }

        if (success) {
            to.writeBytes(from); //copy the rest
            byte[] newData = new byte[to.readableBytes()];
            to.readBytes(newData);
            packet.setData(newData);
        }
        from.release();
        to.release();
        return success;
    }

    @Override
    public boolean handle(UpdateBlockPacket packet) {
        int runtimeId = packet.getRuntimeId();
        packet.setRuntimeId(this.getPaletteRewrite().map(runtimeId));
        return true;
    }

    @Override
    public boolean handle(LevelEventPacket packet) {
        LevelEventType type = packet.getType();

        //TODO: what is LEVEL_EVENT_EVENT_PARTICLE_PUNCH_BLOCK
        switch (type) {
            case PARTICLE_TERRAIN:
            case PARTICLE_DESTROY_BLOCK:
                break;
            default:
                return false;
        }

        int data = packet.getData();
        int high = data & 0xFFFF0000;
        int blockID = this.getPaletteRewrite().map(data & 0xFFFF) & 0xFFFF;
        packet.setData(high | blockID);
        return true;
    }

    @Override
    public boolean handle(LevelSoundEventPacket packet) {
        if (packet.getSound() != SoundEvent.PLACE) {
            return false;
        }

        int data = packet.getExtraData();
        packet.setExtraData(this.getPaletteRewrite().map(data));
        return true;
    }

    @Override
    public boolean handle(AddEntityPacket packet) {
        if (!packet.getIdentifier().equals("minecraft:falling_block")) {
            return false;
        }

        EntityDataMap metaData = packet.getMetadata();
        int runtimeId = metaData.getInt(EntityData.VARIANT);
        metaData.putInt(EntityData.VARIANT, this.getPaletteRewrite().map(runtimeId));
        return true;
    }
}
