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

import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.BedrockSession;
import com.nukkitx.protocol.bedrock.data.GameRuleData;
import com.nukkitx.protocol.bedrock.data.GameType;
import com.nukkitx.protocol.bedrock.data.LevelEventType;
import com.nukkitx.protocol.bedrock.packet.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class PlayerRewriteUtils {

    public static long rewriteId(long from, long rewritten, long origin) {
        return from == origin ? rewritten : (from == rewritten ? origin : from);
    }

    public static void injectChunkPublisherUpdate(BedrockSession session, Vector3i defaultSpawn) {
        NetworkChunkPublisherUpdatePacket packet = new NetworkChunkPublisherUpdatePacket();
        packet.setPosition(defaultSpawn);
        packet.setRadius(160);
        session.sendPacket(packet);
    }

    public static void injectGameMode(BedrockSession session, GameType gameMode) {
        SetPlayerGameTypePacket packet = new SetPlayerGameTypePacket();
        packet.setGamemode(gameMode.ordinal());
        session.sendPacket(packet);
    }

    public static void injectGameRules(BedrockSession session, List<GameRuleData<?>> gameRules) {
        GameRulesChangedPacket packet = new GameRulesChangedPacket();
        packet.getGameRules().addAll(gameRules);
        session.sendPacket(packet);
    }

    public static void injectClearWeather(BedrockSession session) {
        LevelEventPacket stopRain = new LevelEventPacket();
        stopRain.setType(LevelEventType.STOP_RAINING);
        session.sendPacket(stopRain);

        LevelEventPacket stopThunder = new LevelEventPacket();
        stopThunder.setType(LevelEventType.STOP_THUNDERSTORM);
        session.sendPacket(stopThunder);
    }

    public static void injectRemoveEntity(BedrockSession session, long runtimeId) {
        RemoveEntityPacket packet = new RemoveEntityPacket();
        packet.setUniqueEntityId(runtimeId);
        session.sendPacket(packet);
    }

    public static void injectRemoveAllPlayers(BedrockSession session, Collection<UUID> playerList) {
        PlayerListPacket packet = new PlayerListPacket();
        packet.setAction(PlayerListPacket.Action.REMOVE);
        List<PlayerListPacket.Entry> entries = new ArrayList<>();
        for (UUID uuid : playerList) {
            entries.add(new PlayerListPacket.Entry(uuid));
        }
        packet.getEntries().addAll(entries);
        session.sendPacket(packet);
    }

    public static void injectRemoveAllEffects(BedrockSession session, long runtimeId) {
        for (int i = 0; i < 28; i++) {
            injectRemoveEntityEffect(session, runtimeId, i);
        }
    }

    public static void injectRemoveEntityEffect(BedrockSession session, long runtimeId, int effect) {
        MobEffectPacket packet = new MobEffectPacket();
        packet.setRuntimeEntityId(runtimeId);
        packet.setEffectId(effect);
        packet.setEvent(MobEffectPacket.Event.REMOVE);
        session.sendPacket(packet);
    }

    public static void injectRemoveObjective(BedrockSession session, String objectiveId) {
        RemoveObjectivePacket packet = new RemoveObjectivePacket();
        packet.setObjectiveId(objectiveId);
        session.sendPacket(packet);
    }
}
