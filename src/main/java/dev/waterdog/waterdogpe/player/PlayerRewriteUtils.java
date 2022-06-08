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

package dev.waterdog.waterdogpe.player;

import com.nukkitx.math.vector.Vector2f;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.network.VarInts;
import com.nukkitx.protocol.bedrock.BedrockSession;
import com.nukkitx.protocol.bedrock.data.GameRuleData;
import com.nukkitx.protocol.bedrock.data.GameType;
import com.nukkitx.protocol.bedrock.data.LevelEventType;
import com.nukkitx.protocol.bedrock.data.ScoreInfo;
import com.nukkitx.protocol.bedrock.data.entity.*;
import com.nukkitx.protocol.bedrock.packet.*;
import dev.waterdog.waterdogpe.network.protocol.ProtocolVersion;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Collection of functions to remove various client-sided data sets when switching servers.
 * For example removing client-sided weather, effects, effect particles etc..
 */
public class PlayerRewriteUtils {

    public static final RequestChunkRadiusPacket defaultChunkRadius = new RequestChunkRadiusPacket();
    public static final ClientCacheStatusPacket defaultCachePacket = new ClientCacheStatusPacket();

    public static final int DIMENSION_OVERWORLD = 0;
    public static final int DIMENSION_NETHER = 1;
    public static final int DIMENSION_END = 2;

    // Current format for 1.18+ versions
    private static final byte[] fakeChunkDataBlameMojang;
    // This are 1.18.30+
    private static final byte[] fakeChunkDataOverworld;
    private static final byte[] fakeChunkDataNether;
    private static final byte[] fakeChunkDataEnd;
    private static final byte[] emptyChunkRaw;

    static {
        defaultChunkRadius.setRadius(8);
        // Here we create hardcoded "empty" chunk which is accepted by client
        // Because client does not accept empty array list we try to hardcode this
        // Keep in mind that this CAN change with newer versions!

        // Biome sections are just hardcoded to 25 without any reason- thank you Mojang
        fakeChunkDataBlameMojang = createChunkData(1, 25);
        // Since 1.18.30 biomes count is equal to max dim height >> 4
        fakeChunkDataOverworld = createChunkData(1, 24);
        fakeChunkDataNether = createChunkData(1, 8);
        fakeChunkDataEnd = createChunkData(1, 16);
        emptyChunkRaw = createChunkDataRaw();
    }

    private static byte[] createChunkDataRaw() {
        final ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(8); // section version
        buffer.writeByte(0); // zero block storages

        byte[] bytes = new byte[buffer.readableBytes()];
        buffer.readBytes(bytes);
        return bytes;
    }

    private static byte[] createChunkData(int sections, int biomeSections) {
        final ByteBuf buffer = Unpooled.buffer();
        for (int i = 0; i < sections; i++) {
            buffer.writeByte(8); // section version
            buffer.writeByte(0); // zero block storages
            // writePalette(buffer, 0); AIR in palette
        }
        // buffer.writeZero(512); // map height - ??
        for (int i = 0; i < biomeSections; i++) {
            writePalette(buffer, 0); // paletted biomes - 1.18
        }
        buffer.writeByte(0); // Borders

        byte[] bytes = new byte[buffer.readableBytes()];
        buffer.readBytes(bytes);
        return bytes;
    }

    private static void writePalette(ByteBuf buffer, int runtimeId) {
        buffer.writeByte((1 << 1) | 1);  // runtime flag and palette id
        buffer.writeZero(512); // 128 * 4
        VarInts.writeInt(buffer, 1); // Palette size
        VarInts.writeInt(buffer, runtimeId);
    }

    public static long rewriteId(long from, long rewritten, long origin) {
        return from == origin ? rewritten : (from == rewritten ? origin : from);
    }

    public static void rewriteEntityMetadata(EntityDataMap entityDataMap, long entityId, long originalEntityId) {
        rewriteEntityProperty(entityDataMap, EntityData.TARGET_EID, entityId, originalEntityId);
        rewriteEntityProperty(entityDataMap, EntityData.OWNER_EID, entityId, originalEntityId);
        rewriteEntityProperty(entityDataMap, EntityData.TRADE_TARGET_EID, entityId, originalEntityId);
        rewriteEntityProperty(entityDataMap, EntityData.LEASH_HOLDER_EID, entityId, originalEntityId);
    }

    public static void rewriteEntityProperty(EntityDataMap map, EntityData targetEntry, long entityId, long originalEntityId) {
        if(map.containsKey(targetEntry)) {
            map.replace(targetEntry, rewriteId(map.getLong(targetEntry), entityId, originalEntityId));
        }
    }

    public static int determineDimensionId(int from, int to) {
        if (from == to) {
            return from == DIMENSION_OVERWORLD ? DIMENSION_NETHER : DIMENSION_OVERWORLD;
        }
        return to;
    }

    public static void injectChunkPublisherUpdate(BedrockSession session, Vector3i defaultSpawn, int radius) {
        if (session == null || session.isClosed()) {
            return;
        }
        NetworkChunkPublisherUpdatePacket packet = new NetworkChunkPublisherUpdatePacket();
        packet.setPosition(defaultSpawn);
        packet.setRadius(radius);
        session.sendPacket(packet);
    }

    public static void injectGameMode(BedrockSession session, GameType gameMode) {
        if (session == null || session.isClosed()) {
            return;
        }
        SetPlayerGameTypePacket packet = new SetPlayerGameTypePacket();
        packet.setGamemode(gameMode.ordinal());
        session.sendPacket(packet);
    }

    public static void injectGameRules(BedrockSession session, List<GameRuleData<?>> gameRules) {
        if (session == null || session.isClosed()) {
            return;
        }
        GameRulesChangedPacket packet = new GameRulesChangedPacket();
        packet.getGameRules().addAll(gameRules);
        session.sendPacket(packet);
    }

    public static void injectClearWeather(BedrockSession session) {
        if (session == null || session.isClosed()) {
            return;
        }
        LevelEventPacket stopRain = new LevelEventPacket();
        stopRain.setType(LevelEventType.STOP_RAINING);
        stopRain.setData(10000);
        stopRain.setPosition(Vector3f.ZERO);
        session.sendPacketImmediately(stopRain);

        LevelEventPacket stopThunder = new LevelEventPacket();
        stopThunder.setData(0);
        stopThunder.setPosition(Vector3f.ZERO);
        stopThunder.setType(LevelEventType.STOP_THUNDERSTORM);
        session.sendPacket(stopThunder);
    }

    public static void injectSetDifficulty(BedrockSession session, int difficulty) {
        if (session == null || session.isClosed()) {
            return;
        }
        SetDifficultyPacket packet = new SetDifficultyPacket();
        packet.setDifficulty(difficulty);
        session.sendPacket(packet);
    }

    public static void injectRemoveEntityLink(BedrockSession session, long vehicleId, long riderId) {
        if (session == null || session.isClosed()) {
            return;
        }
        SetEntityLinkPacket packet = new SetEntityLinkPacket();
        packet.setEntityLink(new EntityLinkData(vehicleId, riderId, EntityLinkData.Type.REMOVE, false, false));
        session.sendPacket(packet);
    }

    public static void injectRemoveEntity(BedrockSession session, long runtimeId) {
        if (session == null || session.isClosed()) {
            return;
        }
        RemoveEntityPacket packet = new RemoveEntityPacket();
        packet.setUniqueEntityId(runtimeId);
        session.sendPacket(packet);
    }

    public static void injectRemoveAllPlayers(BedrockSession session, Collection<UUID> playerList) {
        if (session == null || session.isClosed()) {
            return;
        }
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
        if (session == null || session.isClosed()) {
            return;
        }
        for (int i = 0; i < 28; i++) {
            injectRemoveEntityEffect(session, runtimeId, i);
        }
        SetEntityDataPacket packet = new SetEntityDataPacket();
        packet.getMetadata().putShort(EntityData.POTION_AUX_VALUE, 0);
        packet.getMetadata().putInt(EntityData.EFFECT_COLOR, 0);
        packet.getMetadata().putByte(EntityData.EFFECT_AMBIENT, (byte) 0);
        packet.setRuntimeEntityId(runtimeId);
        session.sendPacket(packet);
    }

    public static void injectRemoveEntityEffect(BedrockSession session, long runtimeId, int effect) {
        MobEffectPacket packet = new MobEffectPacket();
        packet.setRuntimeEntityId(runtimeId);
        packet.setEffectId(effect);
        packet.setEvent(MobEffectPacket.Event.REMOVE);
        session.sendPacket(packet);
    }

    public static void injectRemoveObjective(BedrockSession session, String objectiveId) {
        if (session == null || session.isClosed()) {
            return;
        }
        RemoveObjectivePacket packet = new RemoveObjectivePacket();
        packet.setObjectiveId(objectiveId);
        session.sendPacket(packet);
    }

    public static void injectRemoveScoreInfos(BedrockSession session, Long2ObjectMap<ScoreInfo> scoreInfos) {
        if (session == null || session.isClosed()) {
            return;
        }
        SetScorePacket packet = new SetScorePacket();
        packet.setAction(SetScorePacket.Action.REMOVE);
        packet.getInfos().addAll(scoreInfos.values());
        session.sendPacket(packet);
    }

    public static void injectRemoveBossbar(BedrockSession session, long bossbarId) {
        if (session == null || session.isClosed()) {
            return;
        }
        BossEventPacket packet = new BossEventPacket();
        packet.setAction(BossEventPacket.Action.REMOVE);
        packet.setBossUniqueEntityId(bossbarId);
        session.sendPacket(packet);
    }

    public static void injectPosition(BedrockSession session, Vector3f position, Vector2f rotation, long runtimeId) {
        if (session == null || session.isClosed()) {
            return;
        }
        MovePlayerPacket packet = new MovePlayerPacket();
        packet.setPosition(position);
        packet.setRuntimeEntityId(runtimeId);
        packet.setRotation(rotation.toVector3(rotation.getY()));
        packet.setMode(MovePlayerPacket.Mode.RESPAWN);
        session.sendPacket(packet);
    }

    public static void injectDimensionChange(BedrockSession session, int dimensionId, Vector3f position, ProtocolVersion version) {
        if (session == null || session.isClosed()){
            return;
        }
        ChangeDimensionPacket packet = new ChangeDimensionPacket();
        packet.setPosition(position);
        packet.setRespawn(true);
        packet.setDimension(dimensionId);
        session.sendPacket(packet);
        injectChunkPublisherUpdate(session, position.toInt(), 3);
        injectEmptyChunks(session, position, 3, dimensionId, version);
    }

    public static void injectEmptyChunks(BedrockSession session, Vector3f spawnPosition, int radius, int dimension, ProtocolVersion version) {
        int chunkPositionX = spawnPosition.getFloorX() >> 4;
        int chunkPositionZ = spawnPosition.getFloorZ() >> 4;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                injectEmptyChunk(session, chunkPositionX + x, chunkPositionZ + z, dimension, version);
            }
        }
    }

    public static void injectEmptyChunk(BedrockSession session, int chunkX, int chunkZ, int dimension, ProtocolVersion version) {
        LevelChunkPacket packet = new LevelChunkPacket();
        packet.setChunkX(chunkX);
        packet.setChunkZ(chunkZ);
        packet.setCachingEnabled(false);
        if (version.isAfterOrEqual(ProtocolVersion.MINECRAFT_PE_1_18_30)) {
            packet.setSubChunksLength(1);
            switch (dimension) {
                case DIMENSION_NETHER:
                    packet.setData(fakeChunkDataNether);
                    break;
                case DIMENSION_END:
                    packet.setData(fakeChunkDataEnd);
                    break;
                case DIMENSION_OVERWORLD:
                default:
                    packet.setData(fakeChunkDataOverworld);
                    break;
            }
        } else if (version.isAfterOrEqual(ProtocolVersion.MINECRAFT_PE_1_18_0)) {
            packet.setSubChunksLength(1);
            packet.setData(fakeChunkDataBlameMojang);
        } else {
            packet.setData(new byte[257]);
        }
        session.sendPacket(packet);
    }

    public static void injectChunkCacheBlobs(BedrockSession session, LongSet blobs) {
        if (session == null || session.isClosed()){
            return;
        }

        ClientCacheMissResponsePacket packet = new ClientCacheMissResponsePacket();
        for (long blob : blobs) {
            packet.getBlobs().put(blob, emptyChunkRaw);
        }
        session.sendPacket(packet);
    }

    public static void injectEntityImmobile(BedrockSession session, long runtimeId, boolean immobile) {
        if (session == null || session.isClosed()){
            return;
        }
        EntityFlags flags = new EntityFlags();
        flags.setFlag(EntityFlag.NO_AI, immobile);
        flags.setFlag(EntityFlag.BREATHING, true); // Hide bubbles
        flags.setFlag(EntityFlag.HAS_GRAVITY, true); // Disable floating

        SetEntityDataPacket packet = new SetEntityDataPacket();
        packet.setRuntimeEntityId(runtimeId);
        packet.getMetadata().putFlags(flags);
        session.sendPacket(packet);
    }

    public static boolean checkForImmobileFlag(EntityDataMap dataMap) {
        return dataMap != null && dataMap.containsKey(EntityData.FLAGS) && dataMap.getFlags().getFlag(EntityFlag.NO_AI);
    }
}
