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
import com.nukkitx.protocol.bedrock.data.inventory.*;
import com.nukkitx.protocol.bedrock.data.inventory.stackrequestactions.CraftResultsDeprecatedStackRequestActionData;
import com.nukkitx.protocol.bedrock.data.inventory.stackrequestactions.StackRequestActionData;
import com.nukkitx.protocol.bedrock.data.inventory.stackrequestactions.StackRequestActionType;
import com.nukkitx.protocol.bedrock.handler.BedrockPacketHandler;
import com.nukkitx.protocol.bedrock.packet.*;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import pe.waterdog.network.rewrite.types.ItemPaletteRewrite;
import pe.waterdog.network.rewrite.types.RewriteData;
import pe.waterdog.player.ProxiedPlayer;

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

    @Override
    public boolean handle(AddItemEntityPacket packet) {
        int runtimeId = packet.getItemInHand().getId();
        packet.getItemInHand().setId(this.translateId(runtimeId));
        return true;
    }

    @Override
    public boolean handle(AddPlayerPacket packet) {
        int runtimeId = packet.getHand().getId();
        packet.getHand().setId(this.translateId(runtimeId));
        return true;
    }

    @Override
    public boolean handle(CraftingDataPacket packet) {
        for (CraftingData craftingData : packet.getCraftingData()){
            if (craftingData.getInputs() != null){
                for (ItemData itemData : craftingData.getInputs()){
                    itemData.setId(this.translateId(itemData.getId()));
                }
            }
            if (craftingData.getOutputs() != null){
                for (ItemData itemData : craftingData.getOutputs()){
                    itemData.setId(this.translateId(itemData.getId()));
                }
            }
        }

        List<PotionMixData> potionMixData = new ObjectArrayList<>();
        for (PotionMixData potion : packet.getPotionMixData()){
            int inputId = this.translateId(potion.getInputId());
            int reagentId = this.translateId(potion.getReagentId());
            int outputId = this.translateId(potion.getOutputId());
            potionMixData.add(new PotionMixData(inputId, potion.getInputMeta(), reagentId, potion.getReagentMeta(), outputId, potion.getOutputMeta()));
        }
        packet.getPotionMixData().clear();
        packet.getPotionMixData().addAll(potionMixData);

        List<ContainerMixData> containerMixData = new ObjectArrayList<>();
        for (ContainerMixData container : packet.getContainerMixData()){
            int inputId = this.translateId(container.getInputId());
            int reagentId = this.translateId(container.getReagentId());
            int outputId = this.translateId(container.getOutputId());
            containerMixData.add(new ContainerMixData(inputId, reagentId, outputId));
        }
        packet.getContainerMixData().clear();
        packet.getContainerMixData().addAll(containerMixData);
        return true;
    }

    @Override
    public boolean handle(CraftingEventPacket packet) {
        for (ItemData itemData : packet.getInputs()){
            itemData.setId(this.translateId(itemData.getId()));
        }

        for (ItemData itemData : packet.getOutputs()){
            itemData.setId(this.translateId(itemData.getId()));
        }
        return true;
    }

    @Override
    public boolean handle(InventoryContentPacket packet) {
        for (ItemData itemData : packet.getContents()){
            itemData.setId(this.translateId(itemData.getId()));
        }
        return true;
    }

    @Override
    public boolean handle(InventorySlotPacket packet) {
        packet.getItem().setId(this.translateId(packet.getItem().getId()));
        return true;
    }

    @Override
    public boolean handle(InventoryTransactionPacket packet) {
        if (packet.getItemInHand() != null){
            packet.getItemInHand().setId(this.translateId(packet.getItemInHand().getId()));
        }

        for (InventoryActionData action : packet.getActions()){
            action.getFromItem().setId(this.translateId(action.getFromItem().getId()));
            action.getToItem().setId(this.translateId(action.getToItem().getId()));
        }
        return true;
    }

    @Override
    public boolean handle(MobEquipmentPacket packet) {
        packet.getItem().setId(this.translateId(packet.getItem().getId()));
        return true;
    }

    @Override
    public boolean handle(MobArmorEquipmentPacket packet) {
        packet.getHelmet().setId(this.translateId(packet.getHelmet().getId()));
        packet.getChestplate().setId(this.translateId(packet.getChestplate().getId()));
        packet.getLeggings().setId(this.translateId(packet.getLeggings().getId()));
        packet.getBoots().setId(this.translateId(packet.getBoots().getId()));
        return true;
    }

    @Override
    public boolean handle(CreativeContentPacket packet) {
        for (ItemData itemData : packet.getContents()){
            itemData.setId(this.translateId(itemData.getId()));
        }
        return true;
    }

    @Override
    public boolean handle(ItemStackRequestPacket packet) {
        boolean changed = false;
        for (ItemStackRequestPacket.Request request : packet.getRequests()){
            for (StackRequestActionData actionData : request.getActions()){
                if (actionData.getType() != StackRequestActionType.CRAFT_RESULTS_DEPRECATED){
                    continue;
                }
                changed = true;
                for (ItemData itemData : ((CraftResultsDeprecatedStackRequestActionData) actionData).getResultItems()){
                    itemData.setId(this.translateId(itemData.getId()));
                }
            }
        }
        return changed;
    }
}
