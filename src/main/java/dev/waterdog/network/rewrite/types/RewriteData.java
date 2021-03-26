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

package dev.waterdog.network.rewrite.types;

import com.google.common.base.Preconditions;
import com.nukkitx.math.vector.Vector2f;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.BlockPropertyData;
import com.nukkitx.protocol.bedrock.data.GameRuleData;
import com.nukkitx.protocol.bedrock.packet.RequestChunkRadiusPacket;
import com.nukkitx.protocol.bedrock.packet.StartGamePacket.ItemEntry;
import dev.waterdog.player.PlayerRewriteUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.List;

/**
 * Rewrite data of a present player.
 * Holds both the client-known entityId and the downstream-known clientId.
 * Important when interacting when packets, as different packet targets might want different entityIds.
 */
public class RewriteData {

    /**
     * The original entityId known to the client
     */
    private long entityId;
    /**
     * The downstream-known entityId
     */
    private long originalEntityId;

    private BlockPalette blockPalette;
    private BlockPaletteRewrite blockPaletteRewrite;
    //TODO: mode blocks rewrite
    private List<BlockPropertyData> blockProperties;

    /**
     * A list of GameRules currently known to the client.
     */
    private List<GameRuleData<?>> gameRules;
    /**
     * The dimensionId the player is currently in
     */
    private int dimension = 0;
    private RequestChunkRadiusPacket chunkRadius = PlayerRewriteUtils.defaultChunkRadius;

    private Vector3f spawnPosition;
    private Vector2f rotation;

    private Object2ObjectMap<String, ItemEntry> itemEntriesMap = new Object2ObjectOpenHashMap<>();
    private Integer shieldBlockingId = null;

    public RewriteData() {
    }

    public long getEntityId() {
        return this.entityId;
    }

    public void setEntityId(long entityId) {
        this.entityId = entityId;
    }

    public long getOriginalEntityId() {
        return this.originalEntityId;
    }

    public void setOriginalEntityId(long originalEntityId) {
        this.originalEntityId = originalEntityId;
    }

    public BlockPalette getBlockPalette() {
        return this.blockPalette;
    }

    public void setBlockPalette(BlockPalette blockPalette) {
        this.blockPalette = blockPalette;
    }

    public BlockPaletteRewrite getBlockPaletteRewrite() {
        return this.blockPaletteRewrite;
    }

    public void setBlockPaletteRewrite(BlockPaletteRewrite paletteRewrite) {
        this.blockPaletteRewrite = paletteRewrite;
    }

    public List<BlockPropertyData> getBlockProperties() {
        return this.blockProperties;
    }

    public void setBlockProperties(List<BlockPropertyData> blockProperties) {
        this.blockProperties = blockProperties;
    }

    public List<GameRuleData<?>> getGameRules() {
        return this.gameRules;
    }

    public void setGameRules(List<GameRuleData<?>> gameRules) {
        this.gameRules = gameRules;
    }

    public int getDimension() {
        return this.dimension;
    }

    public void setDimension(int dimension) {
        this.dimension = dimension;
    }

    public RequestChunkRadiusPacket getChunkRadius() {
        return this.chunkRadius;
    }

    public void setChunkRadius(RequestChunkRadiusPacket chunkRadius) {
        this.chunkRadius = chunkRadius;
    }

    public Vector3f getSpawnPosition() {
        return this.spawnPosition;
    }

    public void setSpawnPosition(Vector3f spawnPosition) {
        this.spawnPosition = spawnPosition;
    }

    public Vector2f getRotation() {
        return this.rotation;
    }

    public void setRotation(Vector2f rotation) {
        this.rotation = rotation;
    }

    public void parseItemIds(List<ItemEntry> itemEntries) {
        Object2ObjectMap<String, ItemEntry> items = new Object2ObjectOpenHashMap<>();
        for (ItemEntry entry : itemEntries) {
            items.put(entry.getIdentifier(), entry);
        }
        this.itemEntriesMap = items;
    }

    public Object2ObjectMap<String, ItemEntry> getItemEntriesMap() {
        return this.itemEntriesMap;
    }

    public void setItemEntriesMap(Object2ObjectMap<String, ItemEntry> itemEntriesMap) {
        this.itemEntriesMap = itemEntriesMap;
    }

    public int getShieldBlockingId() {
        if (this.shieldBlockingId != null) {
            return this.shieldBlockingId;
        }
        ItemEntry itemEntry = this.itemEntriesMap.get("minecraft:shield");
        Preconditions.checkNotNull(itemEntry, "Block shield id can not be null!");
        return itemEntry.getId();
    }

    public void setShieldBlockingId(Integer shieldBlockingId) {
        this.shieldBlockingId = shieldBlockingId;
    }
}
