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
import com.nukkitx.protocol.bedrock.BedrockSession;
import com.nukkitx.protocol.bedrock.packet.ChangeDimensionPacket;
import com.nukkitx.protocol.bedrock.packet.NetworkChunkPublisherUpdatePacket;
import com.nukkitx.protocol.bedrock.packet.PlayStatusPacket;
import com.nukkitx.protocol.bedrock.packet.SetPlayerGameTypePacket;

public class PlayerRewriteUtils {

    public static void injectDimensionChange(BedrockSession session, int dimension){
        if (session.isClosed()) return;

        ChangeDimensionPacket packet = new ChangeDimensionPacket();
        packet.setDimension(dimension);
        packet.setPosition(Vector3f.from(0, 300, 0));
        packet.setRespawn(true);
        session.sendPacket(packet);
    }

    public static void injectStatusChange(BedrockSession session, PlayStatusPacket.Status status){
        PlayStatusPacket packet = new PlayStatusPacket();
        packet.setStatus(status);
        session.sendPacket(packet);
    }

    public static void injectChunkPublisherUpdate(BedrockSession session, Vector3i defaultSpawn){
        if (session.isClosed()) return;

        NetworkChunkPublisherUpdatePacket packet = new NetworkChunkPublisherUpdatePacket();
        packet.setPosition(defaultSpawn);
        packet.setRadius(160);
        session.sendPacket(packet);
    }

    public static void injectGameMode(BedrockSession session, int gameMode){
        if (session.isClosed()) return;

        SetPlayerGameTypePacket packet = new SetPlayerGameTypePacket();
        packet.setGamemode(gameMode);
        session.sendPacket(packet);
    }

    public static long rewriteId(long from, long rewritten, long origin){
        return from == origin? rewritten : (from == rewritten? origin : from);
    }
}
