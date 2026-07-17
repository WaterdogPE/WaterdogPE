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

package dev.waterdog.waterdogpe.network.protocol.user;

import dev.waterdog.waterdogpe.network.connection.ProxiedConnection;
import dev.waterdog.waterdogpe.network.connection.codec.batch.BatchFlags;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.*;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataMap;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityLinkData;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType;
import org.cloudburstmc.protocol.bedrock.netty.BedrockBatchWrapper;
import org.cloudburstmc.protocol.bedrock.packet.*;
import dev.waterdog.waterdogpe.network.protocol.ProtocolVersion;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.cloudburstmc.protocol.common.util.VarInts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
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

    private static final int INPUT_LOCK_CAMERA = 1 << 1;
    private static final int INPUT_LOCK_MOVEMENT = 1 << 2;
    public static final int INPUT_LOCK_FREEZE = INPUT_LOCK_CAMERA | INPUT_LOCK_MOVEMENT;

    // Current format for 1.18+ versions
    private static final ByteBuf fakeChunkDataBlameMojang;
    // This are 1.18.30+
    private static final ByteBuf fakeChunkDataOverworld;
    private static final ByteBuf fakeChunkDataNether;
    private static final ByteBuf fakeChunkDataEnd;
    private static final ByteBuf emptyChunkRaw;

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

    private static ByteBuf createChunkDataRaw() {
        final ByteBuf buffer = Unpooled.directBuffer();
        buffer.writeByte(8); // section version
        buffer.writeByte(0); // zero block storages
        return buffer.asReadOnly();
    }

    private static ByteBuf createChunkData(int sections, int biomeSections) {
        final ByteBuf buffer = Unpooled.buffer();
        for (int i = 0; i < sections; i++) {
            buffer.writeByte(8); // section version
            buffer.writeByte(0); // zero block storages
            // writePalette(buffer, 0); AIR in palette
        }
        // buffer.writeZero(512); // map height - ??
        // paletted biomes - 1.18
        writePalette(buffer, 0);
        for (int i = 1; i < biomeSections; i++) {
            buffer.writeByte((127 << 1) | 1); // link to previous biome palette
        }
        buffer.writeByte(0); // Borders
        return buffer.asReadOnly();
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

    public static int determineDimensionId(int from, int to) {
        if (from == to) {
            return from == DIMENSION_OVERWORLD ? DIMENSION_NETHER : DIMENSION_OVERWORLD;
        }
        return to;
    }

    public static void injectChunkPublisherUpdate(ProxiedConnection session, Vector3i center, int chunkRadius) {
        if (session == null || !session.isConnected()) {
            return;
        }
        NetworkChunkPublisherUpdatePacket packet = new NetworkChunkPublisherUpdatePacket();
        packet.setPosition(center);
        packet.setRadius(chunkRadius << 4); // radius is in blocks, not chunks
        session.sendPacketImmediately(packet);
    }

    public static void injectGameMode(ProxiedConnection session, GameType gameMode) {
        if (session == null || !session.isConnected()) {
            return;
        }
        SetPlayerGameTypePacket packet = new SetPlayerGameTypePacket();
        packet.setGamemode(gameMode.ordinal());
        session.sendPacket(packet);
    }

    public static void injectGameRules(ProxiedConnection session, List<GameRuleData<?>> gameRules) {
        if (session == null || !session.isConnected()) {
            return;
        }
        GameRulesChangedPacket packet = new GameRulesChangedPacket();
        packet.getGameRules().addAll(gameRules);
        session.sendPacket(packet);
    }

    public static void injectClearWeather(ProxiedConnection session) {
        if (session == null || !session.isConnected()) {
            return;
        }
        LevelEventPacket stopThunder = new LevelEventPacket();
        stopThunder.setData(0);
        stopThunder.setPosition(Vector3f.ZERO);
        stopThunder.setType(LevelEvent.STOP_THUNDERSTORM);
        session.sendPacket(stopThunder);

        LevelEventPacket stopRain = new LevelEventPacket();
        stopRain.setType(LevelEvent.STOP_RAINING);
        stopRain.setData(10000);
        stopRain.setPosition(Vector3f.ZERO);
        session.sendPacket(stopRain);
    }

    public static void injectSetDifficulty(ProxiedConnection session, int difficulty) {
        if (session == null || !session.isConnected()) {
            return;
        }
        SetDifficultyPacket packet = new SetDifficultyPacket();
        packet.setDifficulty(difficulty);
        session.sendPacket(packet);
    }

    public static void injectTime(ProxiedConnection session, int time) {
        if (session == null || !session.isConnected()) {
            return;
        }
        SetTimePacket packet = new SetTimePacket();
        packet.setTime(time);
        session.sendPacket(packet);
    }

    public static void injectRemoveEntityLink(ProxiedConnection session, long vehicleId, long riderId) {
        if (session == null || !session.isConnected()) {
            return;
        }
        SetEntityLinkPacket packet = new SetEntityLinkPacket();
        packet.setEntityLink(new EntityLinkData(vehicleId, riderId, EntityLinkData.Type.REMOVE, false, false));
        session.sendPacket(packet);
    }

    public static void injectRemoveEntity(ProxiedConnection session, long runtimeId) {
        if (session == null || !session.isConnected()) {
            return;
        }
        RemoveEntityPacket packet = new RemoveEntityPacket();
        packet.setUniqueEntityId(runtimeId);
        session.sendPacket(packet);
    }

    public static void injectRemoveVolumeEntity(ProxiedConnection session, int id, int dimension) {
        if (session == null || !session.isConnected()) {
            return;
        }
        RemoveVolumeEntityPacket packet = new RemoveVolumeEntityPacket();
        packet.setId(id);
        packet.setDimension(dimension);
        session.sendPacket(packet);
    }

    public static void injectClearFog(ProxiedConnection session) {
        if (session == null || !session.isConnected()) {
            return;
        }
        session.sendPacket(new PlayerFogPacket()); // empty fog stack clears all fog
    }

    public static void injectInputLocks(ProxiedConnection session, int lockData, Vector3f position) {
        if (session == null || !session.isConnected()) {
            return;
        }
        UpdateClientInputLocksPacket packet = new UpdateClientInputLocksPacket();
        packet.setLockComponentData(lockData); // 0 unlocks; position only anchors a movement lock
        packet.setServerPosition(position);
        session.sendPacket(packet);
    }

    public static void injectResetHud(ProxiedConnection session, Set<HudElement> elements) {
        if (session == null || !session.isConnected()) {
            return;
        }
        SetHudPacket packet = new SetHudPacket();
        packet.getElements().addAll(elements);
        packet.setVisibility(HudVisibility.RESET);
        session.sendPacket(packet);
    }

    public static void injectCloseContainer(ProxiedConnection session, byte id, ContainerType type) {
        if (session == null || !session.isConnected()) {
            return;
        }
        ContainerClosePacket packet = new ContainerClosePacket();
        packet.setId(id);
        packet.setType(type);
        packet.setServerInitiated(true);
        session.sendPacket(packet);
    }

    public static void injectRemoveAllPlayers(ProxiedConnection session, Collection<UUID> playerList) {
        if (session == null || !session.isConnected()) {
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

    public static void injectRemoveAllEffects(ProxiedConnection session, long runtimeId, ProtocolVersion version) {
        if (session == null || !session.isConnected()) {
            return;
        }

        int effectsCount = version.isAfter(ProtocolVersion.MINECRAFT_PE_1_19_0) ? 30 : 28;
        for (int i = 0; i < effectsCount; i++) {
            injectRemoveEntityEffect(session, runtimeId, i);
        }
        SetEntityDataPacket packet = new SetEntityDataPacket();
        packet.getMetadata().putType(EntityDataTypes.AUX_VALUE_DATA, (short) 0);
        packet.getMetadata().putType(EntityDataTypes.EFFECT_COLOR, 0);
        packet.getMetadata().putType(EntityDataTypes.EFFECT_AMBIENCE, (byte) 0);
        packet.setRuntimeEntityId(runtimeId);
        session.sendPacket(packet);
    }

    public static void injectRemoveEntityEffect(ProxiedConnection session, long runtimeId, int effect) {
        MobEffectPacket packet = new MobEffectPacket();
        packet.setRuntimeEntityId(runtimeId);
        packet.setEffectId(effect);
        packet.setEvent(MobEffectPacket.Event.REMOVE);
        session.sendPacket(packet);
    }

    public static void injectRemoveObjective(ProxiedConnection session, String objectiveId) {
        if (session == null || !session.isConnected()) {
            return;
        }
        RemoveObjectivePacket packet = new RemoveObjectivePacket();
        packet.setObjectiveId(objectiveId);
        session.sendPacket(packet);
    }

    public static void injectRemoveScoreInfos(ProxiedConnection session, Long2ObjectMap<ScoreInfo> scoreInfos) {
        if (session == null || !session.isConnected()) {
            return;
        }
        SetScorePacket packet = new SetScorePacket();
        packet.setAction(SetScorePacket.Action.REMOVE);
        packet.getInfos().addAll(scoreInfos.values());
        session.sendPacket(packet);
    }

    public static void injectRemoveBossbar(ProxiedConnection session, long bossbarId) {
        if (session == null || !session.isConnected()) {
            return;
        }
        BossEventPacket packet = new BossEventPacket();
        packet.setAction(BossEventPacket.Action.REMOVE);
        packet.setBossUniqueEntityId(bossbarId);
        session.sendPacket(packet);
    }

    public static void injectPosition(ProxiedConnection session, Vector3f position, Vector2f rotation, long runtimeId) {
        if (session == null || !session.isConnected()) {
            return;
        }
        MovePlayerPacket packet = new MovePlayerPacket();
        packet.setPosition(position);
        packet.setRuntimeEntityId(runtimeId);
        packet.setRotation(rotation.toVector3(rotation.getY()));
        packet.setMode(MovePlayerPacket.Mode.RESPAWN);
        session.sendPacketImmediately(packet);
    }

    public static void injectDimensionChange(ProxiedConnection session, int dimensionId, Vector3f position, long runtimeId, ProtocolVersion version, boolean chunks, boolean requestSubChunks) {
        if (session == null || !session.isConnected()) {
            return;
        }
        ChangeDimensionPacket packet = new ChangeDimensionPacket();
        packet.setPosition(position);
        packet.setRespawn(true);
        packet.setDimension(dimensionId);
        session.sendPacketImmediately(packet);

        if (chunks) {
            injectChunkPublisherUpdate(session, position.toInt(), 3);
            injectEmptyChunks(session, position, 3, dimensionId, version, requestSubChunks);
        }

        if (version.isAfterOrEqual(ProtocolVersion.MINECRAFT_PE_1_19_50)) {
            // The game does for some unknown reason expect client dim change ACK
            // to be sent from server in order to fully finish the transfer
            PlayerActionPacket actionPacket = new PlayerActionPacket();
            actionPacket.setRuntimeEntityId(runtimeId);
            actionPacket.setAction(PlayerActionType.DIMENSION_CHANGE_SUCCESS);
            actionPacket.setBlockPosition(Vector3i.ZERO);
            actionPacket.setResultPosition(Vector3i.ZERO);
            actionPacket.setFace(0);
            session.sendPacketImmediately(actionPacket);
        }
    }

    public static void injectEmptyChunks(ProxiedConnection session, Vector3f spawnPosition, int radius, int dimension, ProtocolVersion version, boolean requestSubChunks) {
        int chunkPositionX = spawnPosition.getFloorX() >> 4;
        int chunkPositionZ = spawnPosition.getFloorZ() >> 4;

        List<BedrockPacket> packets = new ObjectArrayList<>();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                packets.add(injectEmptyChunk(chunkPositionX + x, chunkPositionZ + z, dimension, version, requestSubChunks));
            }
        }

        BedrockBatchWrapper wrapper = BedrockBatchWrapper.create(session.getSubClientId(), packets.toArray(new BedrockPacket[0]));
        wrapper.setFlag(BatchFlags.SKIP_QUEUE);
        session.sendPacket(wrapper);
    }

    public static LevelChunkPacket injectEmptyChunk(int chunkX, int chunkZ, int dimension, ProtocolVersion version, boolean requestSubChunks) {
        LevelChunkPacket packet = new LevelChunkPacket();
        packet.setChunkX(chunkX);
        packet.setChunkZ(chunkZ);
        packet.setCachingEnabled(false);
        packet.setDimension(dimension);
        // Request mode is only serializable since 1.18.30 (v486 codec); older codecs ignore the flag
        if (requestSubChunks && version.isAfterOrEqual(ProtocolVersion.MINECRAFT_PE_1_18_30)) {
            packet.setRequestSubChunks(true);
            packet.setSubChunkLimit(switch (dimension) {
                case DIMENSION_NETHER -> 7;
                case DIMENSION_END -> 15;
                default -> 23;
            });
            packet.setData(Unpooled.EMPTY_BUFFER);
        } else if (version.isAfterOrEqual(ProtocolVersion.MINECRAFT_PE_1_18_30)) {
            packet.setSubChunksLength(1);
            switch (dimension) {
                case DIMENSION_NETHER -> packet.setData(fakeChunkDataNether.retainedSlice());
                case DIMENSION_END -> packet.setData(fakeChunkDataEnd.retainedSlice());
                default -> packet.setData(fakeChunkDataOverworld.retainedSlice());
            }
        } else if (version.isAfterOrEqual(ProtocolVersion.MINECRAFT_PE_1_18_0)) {
            packet.setSubChunksLength(1);
            packet.setData(fakeChunkDataBlameMojang.retainedSlice());
        } else {
            packet.setData(Unpooled.wrappedBuffer(new byte[257]));
        }
        return packet;
    }

    public static void injectChunkCacheBlobs(ProxiedConnection session, LongSet blobs) {
        if (session == null || !session.isConnected()){
            return;
        }

        ClientCacheMissResponsePacket packet = new ClientCacheMissResponsePacket();
        for (long blob : blobs) {
            packet.getBlobs().put(blob, emptyChunkRaw);
        }
        session.sendPacket(packet);
    }

    /**
     * Answers a client sub-chunk request with all-air sub-chunks. Used during a transfer to satisfy the
     * requests triggered by our injected request-mode empty chunks, so the client finishes loading the
     * spawn column and sends DIMENSION_CHANGE_SUCCESS instead of waiting on the not-yet-wired new server.
     */
    public static void injectAirSubChunkResponse(ProxiedConnection session, SubChunkRequestPacket request) {
        if (session == null || !session.isConnected()) {
            return;
        }

        SubChunkPacket packet = new SubChunkPacket();
        packet.setDimension(request.getDimension());
        packet.setCacheEnabled(false);
        packet.setCenterPosition(request.getSubChunkPosition());

        List<Vector3i> offsets = request.getPositionOffsets();
        if (offsets.isEmpty()) {
            offsets = List.of(Vector3i.ZERO); // pre-v485 clients request a single sub-chunk with no offsets
        }
        for (Vector3i offset : offsets) {
            SubChunkData data = new SubChunkData();
            data.setPosition(offset);
            data.setResult(SubChunkRequestResult.SUCCESS_ALL_AIR);
            data.setData(Unpooled.EMPTY_BUFFER);
            data.setHeightMapType(HeightMapDataType.NO_DATA);
            data.setRenderHeightMapType(HeightMapDataType.NO_DATA); // serialized since v818
            packet.getSubChunks().add(data);
        }
        session.sendPacketImmediately(packet);
    }

    public static void injectEntityImmobile(ProxiedConnection session, long runtimeId, boolean immobile) {
        if (session == null || !session.isConnected()){
            return;
        }

        SetEntityDataPacket packet = new SetEntityDataPacket();
        packet.setRuntimeEntityId(runtimeId);
        packet.getMetadata().setFlag(EntityFlag.NO_AI, immobile);
        packet.getMetadata().setFlag(EntityFlag.BREATHING, true); // Hide bubbles
        packet.getMetadata().setFlag(EntityFlag.HAS_GRAVITY, true); // Disable floating
        packet.getMetadata().setFlag(EntityFlag.SLEEPING, false); // Wake from the forced inventory close, see injectForceCloseInventory
        session.sendPacketImmediately(packet);
    }

    public static void injectForceCloseInventory(ProxiedConnection session, long runtimeId) {
        if (session == null || !session.isConnected()){
            return;
        }
        // The client closes every open inventory, including its own window which ContainerClosePacket can not close,
        // when the SLEEPING flag is set. Cleared again by injectEntityImmobile once the transfer settles.
        SetEntityDataPacket packet = new SetEntityDataPacket();
        packet.setRuntimeEntityId(runtimeId);
        packet.getMetadata().setFlag(EntityFlag.SLEEPING, true);
        session.sendPacketImmediately(packet);
    }

    public static boolean checkForImmobileFlag(EntityDataMap dataMap) {
        return dataMap != null && dataMap.getFlags() != null && Boolean.TRUE.equals(dataMap.getFlags().get(EntityFlag.NO_AI));
    }
}
