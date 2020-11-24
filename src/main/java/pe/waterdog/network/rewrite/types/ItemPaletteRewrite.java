/*
 * Copyright 2020 WaterdogTEAM
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package pe.waterdog.network.rewrite.types;

public class ItemPaletteRewrite {

    public static ItemPaletteRewrite ITEM_EQUAL = new ItemPaletteRewrite(null, null) {
        @Override
        public int fromDownstream(int runtimeId) {
            return runtimeId;
        }

        @Override
        public int fromUpstream(int runtimeId) {
            return runtimeId;
        }
    };

    private final ItemPalette upstreamPalette;
    private final ItemPalette downstreamPalette;

    public ItemPaletteRewrite(ItemPalette upstreamPalette, ItemPalette downstreamPalette) {
        this.upstreamPalette = upstreamPalette;
        this.downstreamPalette = downstreamPalette;
    }

    public int fromDownstream(int runtimeId){
        ItemEntry itemEntry = this.downstreamPalette.getEntry(runtimeId);
        return itemEntry == null? 0 : this.upstreamPalette.getId(itemEntry.getIdentifier());
    }

    public int fromUpstream(int runtimeId){
        ItemEntry itemEntry = this.upstreamPalette.getEntry(runtimeId);
        return itemEntry == null? 0 : this.downstreamPalette.getId(itemEntry.getIdentifier());
    }
}
