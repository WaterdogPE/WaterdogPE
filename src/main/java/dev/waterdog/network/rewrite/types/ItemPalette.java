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
import com.nukkitx.protocol.bedrock.packet.StartGamePacket;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.*;
import it.unimi.dsi.fastutil.shorts.Short2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import dev.waterdog.network.protocol.ProtocolVersion;

import java.util.List;

public class ItemPalette {

    public static final int OLD_SHIELD_ID = 513;

    private static final Int2ObjectMap<ItemPalette> paletteCache = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<ItemEntry> networkToEntry = new Int2ObjectOpenHashMap<>();
    private final Object2ObjectMap<String, ItemEntry> identifierToEntry = new Object2ObjectOpenHashMap<>();

    private Short shieldBlockingId = null;
    private Short downstreamShieldBlockingId = null;

    public ItemPalette(List<StartGamePacket.ItemEntry> itemEntries, ProtocolVersion protocol){
        for (StartGamePacket.ItemEntry entry : itemEntries){
            if (entry.getIdentifier().equals("minecraft:shield")){
                this.shieldBlockingId = entry.getId();
                this.downstreamShieldBlockingId = entry.getId();
            }
            this.addEntry(entry.getId(), entry.getIdentifier());
        }
        Preconditions.checkNotNull(this.shieldBlockingId, "Tried to parse shield blocking id from invalid item palette!");
    }

    public static ItemPalette getPalette(List<StartGamePacket.ItemEntry> itemEntries, ProtocolVersion protocol) {
        int hashId = itemEntries.hashCode();
        if (paletteCache.containsKey(hashId)) {
            return paletteCache.get(hashId);
        }

        ItemPalette palette = new ItemPalette(itemEntries, protocol);
        paletteCache.put(hashId, palette);
        return palette;
    }

    public ItemPaletteRewrite createRewrite(ItemPalette upstreamPalette) {
        if (ItemPalette.this == upstreamPalette) {
            return ItemPaletteRewrite.ITEM_EQUAL;
        }
        return new ItemPaletteRewrite(upstreamPalette, this);
    }

    private void addEntry(int runtimeId, String identifier) {
        final ItemEntry entry = new ItemEntry(identifier, runtimeId);
        this.identifierToEntry.put(identifier, entry);
        this.networkToEntry.put(runtimeId, entry);
    }

    public ItemEntry getEntry(String identifier) {
        return this.identifierToEntry.get(identifier);
    }

    public ItemEntry getEntry(int runtimeId) {
        return this.networkToEntry.get(runtimeId);
    }

    public int getId(String identifier) {
        ItemEntry itemEntry = this.identifierToEntry.get(identifier);
        return itemEntry == null? 0 : itemEntry.getRuntimeId();
    }

    public Short getShieldBlockingId() {
        return this.shieldBlockingId;
    }

    public void setDownstreamShieldBlockingId(Short downstreamShieldBlockingId) {
        this.downstreamShieldBlockingId = downstreamShieldBlockingId;
    }

    public Short getDownstreamShieldBlockingId(){
        return this.downstreamShieldBlockingId;
    }
}
