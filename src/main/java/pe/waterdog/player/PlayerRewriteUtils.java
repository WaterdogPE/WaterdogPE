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

package pe.waterdog.player;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.network.VarInts;
import com.nukkitx.protocol.bedrock.BedrockPacket;
import com.nukkitx.protocol.bedrock.BedrockSession;
import com.nukkitx.protocol.bedrock.data.GameType;
import com.nukkitx.protocol.bedrock.packet.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class PlayerRewriteUtils {

    private static final int PLAYER_ACTION_ID = 36;
    protected static final int DIM_SWITCH_ACK_ID = -100;
    protected static final int DIMENSION_CHANGE_ACK = 14;

    private static final byte[] fakePEChunkData;
    
    static {
        final ByteBuf serializer = Unpooled.buffer();
        final ByteBuf chunkdata = Unpooled.buffer();

        chunkdata.writeByte(1); //1 section
        chunkdata.writeByte(8); //New subchunk version!
        chunkdata.writeByte(1); //Zero blockstorages :O
        chunkdata.writeByte((1 << 1) | 1);  //Runtimeflag and palette id.
        chunkdata.writeZero(512);
        VarInts.writeUnsignedInt(chunkdata, 1); //Palette size
        VarInts.writeUnsignedInt(chunkdata, 0); //Air
        chunkdata.writeZero(512); //heightmap.
        chunkdata.writeZero(256); //Biomedata.
        chunkdata.writeByte(0); //borders

        //chunks
        chunkdata.markReaderIndex();
        VarInts.writeInt(serializer, chunkdata.readUnsignedByte());
        serializer.writeByte(0);
        VarInts.writeInt(serializer, chunkdata.readableBytes());
        serializer.writeBytes(chunkdata);
        fakePEChunkData = new byte[serializer.readableBytes()];
        serializer.readBytes(fakePEChunkData);
    }

    public static void injectDimensionChange(BedrockSession session, int dimension){
        if (session.isClosed()) return;

        ChangeDimensionPacket packet = new ChangeDimensionPacket();
        packet.setDimension(dimension);
        packet.setPosition(Vector3f.from(0, 300, 0));
        packet.setRespawn(true);
        session.sendPacketImmediately(packet);

        forceDimensionChange(session);
    }

    public static void forceDimensionChange(BedrockSession session){
        injectChunkPublisherUpdate(session, Vector3i.from(0, 0, 0));
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                LevelChunkPacket chunkPacket = new LevelChunkPacket();
                chunkPacket.setChunkX(x);
                chunkPacket.setChunkZ(z);
                chunkPacket.setData(fakePEChunkData);
                session.sendPacketImmediately(chunkPacket);
            }
        }
    }

    public static void injectStatusChange(BedrockSession session, PlayStatusPacket.Status status){
        PlayStatusPacket packet = new PlayStatusPacket();
        packet.setStatus(status);
        session.sendPacketImmediately(packet);
    }

    public static void injectChunkPublisherUpdate(BedrockSession session, Vector3i defaultSpawn){
        if (session.isClosed()) return;

        NetworkChunkPublisherUpdatePacket packet = new NetworkChunkPublisherUpdatePacket();
        packet.setPosition(defaultSpawn);
        packet.setRadius(160);
        session.sendPacketImmediately(packet);
    }

    public static void injectGameMode(BedrockSession session, GameType gameMode){
        if (session.isClosed()) return;

        SetPlayerGameTypePacket packet = new SetPlayerGameTypePacket();
        packet.setGamemode(gameMode.ordinal());
        session.sendPacketImmediately(packet);
    }

    public static long rewriteId(long from, long rewritten, long origin){
        return from == origin? rewritten : (from == rewritten? origin : from);
    }
}
