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

package pe.waterdog.utils;

import com.nukkitx.nbt.tag.CompoundTag;
import com.nukkitx.nbt.tag.ListTag;
import com.nukkitx.protocol.bedrock.data.GameRuleData;

import java.util.List;

public class PlayerRewriteData {

    private final long entityId;
    private final ListTag<CompoundTag> blockPallete;

    private List<GameRuleData<?>> gameRules;

    public PlayerRewriteData(long entityId, ListTag<CompoundTag> blockPallete, List<GameRuleData<?>> gameRules){
        this.entityId = entityId;
        this.blockPallete = blockPallete;
        this.gameRules = gameRules;
    }

    public long getEntityId() {
        return this.entityId;
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
}
