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

package pe.waterdog.network.session;

import com.nukkitx.nbt.tag.CompoundTag;
import com.nukkitx.nbt.tag.ListTag;
import com.nukkitx.protocol.bedrock.data.GameRuleData;

import java.util.List;

public class RewriteData {

    private final long entityId;
    private long originalEntityId;
    private final ListTag<CompoundTag> blockPallete;
    private List<GameRuleData<?>> gameRules;
    private int dimension = 0;

    public RewriteData(long entityId, long originalEntityId, ListTag<CompoundTag> blockPallete, List<GameRuleData<?>> gameRules, int dimension){
        this.entityId = entityId;
        this.originalEntityId = originalEntityId;
        this.blockPallete = blockPallete;
        this.gameRules = gameRules;
        this.dimension = dimension;
    }

    public long getEntityId() {
        return this.entityId;
    }

    public long getOriginalEntityId() {
        return this.originalEntityId;
    }

    public void setOriginalEntityId(long originalEntityId) {
        this.originalEntityId = originalEntityId;
    }

    public ListTag<CompoundTag> getBlockPallete() {
        return this.blockPallete;
    }

    public void setGameRules(List<GameRuleData<?>> gameRules) {
        this.gameRules = gameRules;
    }

    public List<GameRuleData<?>> getGameRules() {
        return this.gameRules;
    }

    public int getDimension() {
        return this.dimension;
    }

    public void setDimension(int dimension) {
        this.dimension = dimension;
    }
}
