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

package pe.waterdog.network.rewrite.types;

import com.nukkitx.math.vector.Vector2f;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.GameRuleData;
import com.nukkitx.protocol.bedrock.packet.RequestChunkRadiusPacket;

import java.util.List;

/**
 * Rewrite data of a present player.
 * Holds both the client-known entityId and the downstream-known clientId.
 * Important when interacting when packets, as different packet targets might want different entityIds.
 */
public class RewriteData {

    /**
     * the downstream-known entityId
     */
    private long entityId;
    /**
     * the original entityId known to the client
     */
    private long originalEntityId;

    private BlockPalette blockPalette;
    private BlockPaletteRewrite paletteRewrite;

    /**
     * A list of GameRules currently known to the client.
     */
    private List<GameRuleData<?>> gameRules;
    /**
     * The dimensionId the player is currently in
     */
    private int dimension = 0;
    private RequestChunkRadiusPacket chunkRadius;

    private Vector3f spawnPosition;
    private Vector2f rotation;

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

    public BlockPaletteRewrite getPaletteRewrite() {
        return this.paletteRewrite;
    }

    public void setPaletteRewrite(BlockPaletteRewrite paletteRewrite) {
        this.paletteRewrite = paletteRewrite;
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

    public void setSpawnPosition(Vector3f spawnPosition) {
        this.spawnPosition = spawnPosition;
    }

    public Vector3f getSpawnPosition() {
        return this.spawnPosition;
    }

    public void setRotation(Vector2f rotation) {
        this.rotation = rotation;
    }

    public Vector2f getRotation() {
        return this.rotation;
    }
}
