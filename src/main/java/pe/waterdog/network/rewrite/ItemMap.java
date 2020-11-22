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

import com.nukkitx.protocol.bedrock.BedrockPacket;
import com.nukkitx.protocol.bedrock.data.inventory.CraftingData;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import com.nukkitx.protocol.bedrock.handler.BedrockPacketHandler;
import com.nukkitx.protocol.bedrock.packet.AddItemEntityPacket;
import com.nukkitx.protocol.bedrock.packet.AddPlayerPacket;
import com.nukkitx.protocol.bedrock.packet.CraftingDataPacket;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import pe.waterdog.network.rewrite.types.ItemPaletteRewrite;
import pe.waterdog.network.rewrite.types.RewriteData;
import pe.waterdog.player.ProxiedPlayer;

import java.util.ArrayList;
import java.util.List;

public class ItemMap implements BedrockPacketHandler {

    protected final ProxiedPlayer player;
    protected final RewriteData rewrite;
    /**
     * If receiving from downstream to upstream.
     */
    private final boolean reverse;

    public ItemMap(ProxiedPlayer player, boolean reverse) {
        this.player = player;
        this.rewrite = player.getRewriteData();
        this.reverse = reverse;
    }

    public ItemPaletteRewrite getPaletteRewrite() {
        return this.rewrite.getItemPaletteRewrite();
    }

    public boolean doRewrite(BedrockPacket packet) {
        return this.player.canRewrite() && packet.handle(this);
    }

    private int translateId(int runtimeId){
        return this.reverse? this.getPaletteRewrite().fromDownstream(runtimeId) : this.getPaletteRewrite().fromUpstream(runtimeId);
    }

    private ItemData duplicate(ItemData item, int runtimeId){
        return ItemData.of(runtimeId, item.getDamage(), item.getCount(), item.getTag(), item.getCanPlace(), item.getCanBreak(), item.getBlockingTicks());
    }

    private ItemData duplicateNet(ItemData item, int runtimeId){
        return ItemData.fromNet(item.getNetId(), runtimeId, item.getDamage(), item.getCount(), item.getTag(), item.getCanPlace(), item.getCanBreak(), item.getBlockingTicks());
    }

    private CraftingData duplicateCraftingData(CraftingData craftingData, List<ItemData> inputs, List<ItemData> outputs){
        return new CraftingData(
                craftingData.getType(),
                craftingData.getRecipeId(),
                craftingData.getWidth(),
                craftingData.getHeight(),
                craftingData.getInputId(), //TODO: verify if this is item id
                craftingData.getInputDamage(),
                inputs.toArray(new ItemData[0]),
                outputs.toArray(new ItemData[0]),
                craftingData.getUuid(),
                craftingData.getCraftingTag(),
                craftingData.getPriority()
        );
    }

    @Override
    public boolean handle(AddItemEntityPacket packet) {
        int runtimeId = packet.getItemInHand().getId();
        packet.setItemInHand(this.duplicate(packet.getItemInHand(), this.translateId(runtimeId)));
        return true;
    }

    @Override
    public boolean handle(AddPlayerPacket packet) {
        int runtimeId = packet.getHand().getId();
        packet.setHand(this.duplicate(packet.getHand(), this.translateId(runtimeId)));
        return true;
    }

    @Override
    public boolean handle(CraftingDataPacket packet) {
        List<CraftingData> craftingList = new ObjectArrayList<>();
        for (CraftingData craftingData : packet.getCraftingData()){
            List<ItemData> inputs = new ArrayList<>();
            for (ItemData itemData : craftingData.getInputs()){
                inputs.add(this.duplicate(itemData, this.translateId(itemData.getId())));
            }

            List<ItemData> outputs = new ArrayList<>();
            for (ItemData itemData : craftingData.getOutputs()){
                outputs.add(this.duplicate(itemData, this.translateId(itemData.getId())));
            }
            craftingList.add(this.duplicateCraftingData(craftingData, inputs, outputs));
        }
        packet.getCraftingData().clear();
        packet.getCraftingData().addAll(craftingList);

        //TODO: potionMixData, containerMixData
        return true;
    }
}
