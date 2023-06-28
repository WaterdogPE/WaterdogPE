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

package dev.waterdog.waterdogpe.network.protocol.rewrite.types;

public class BlockPaletteRewrite {

    public static final BlockPaletteRewrite BLOCK_EQUAL = new BlockPaletteRewrite(null, null) {
        @Override
        public int fromDownstream(int runtimeId) {
            return runtimeId;
        }
    };

    private final BlockPalette upstreamPalette;
    private final BlockPalette downstreamPalette;

    public BlockPaletteRewrite(BlockPalette upstreamPalette, BlockPalette downstreamPalette) {
        this.upstreamPalette = upstreamPalette;
        this.downstreamPalette = downstreamPalette;
    }

    public int fromDownstream(int runtimeId) {
        return this.upstreamPalette.getId(this.downstreamPalette.getEntry(runtimeId));
    }
}
