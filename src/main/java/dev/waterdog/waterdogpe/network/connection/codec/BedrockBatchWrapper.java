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

package dev.waterdog.waterdogpe.network.connection.codec;

import dev.waterdog.waterdogpe.network.connection.codec.compression.CompressionAlgorithm;
import io.netty.buffer.ByteBuf;
import io.netty.util.AbstractReferenceCounted;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.ReferenceCounted;
import io.netty.util.internal.ObjectPool;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.cloudburstmc.protocol.bedrock.data.PacketCompressionAlgorithm;
import org.cloudburstmc.protocol.bedrock.netty.BedrockPacketWrapper;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class BedrockBatchWrapper extends AbstractReferenceCounted {
    private static final ObjectPool<BedrockBatchWrapper> RECYCLER = ObjectPool.newPool(BedrockBatchWrapper::new);
    private final ObjectPool.Handle<BedrockBatchWrapper> handle;

    private ByteBuf compressed;
    private CompressionAlgorithm algorithm;

    private ByteBuf uncompressed;
    private List<BedrockPacketWrapper> packets;

    private boolean modified;
    private boolean skipQueue;

    private BedrockBatchWrapper(ObjectPool.Handle<BedrockBatchWrapper> handle) {
        this.handle = handle;
    }

    public static BedrockBatchWrapper newInstance() {
        return newInstance(null, null);
    }

    public static BedrockBatchWrapper newInstance(ByteBuf compressed, ByteBuf uncompressed) {
        BedrockBatchWrapper batch = RECYCLER.get();
        batch.compressed = compressed;
        batch.uncompressed = uncompressed;
        batch.packets = new ObjectArrayList<>();
        batch.setRefCnt(1);
        batch.modified = false;
        batch.skipQueue = false;
        batch.algorithm = null;
        return batch;
    }

    public static BedrockBatchWrapper create(int subClientId, BedrockPacket... packets) {
        BedrockBatchWrapper batch = BedrockBatchWrapper.newInstance();
        for (BedrockPacket packet : packets) {
            batch.getPackets().add(new BedrockPacketWrapper(0, subClientId, 0, packet, null));
        }
        return batch;
    }

    @Override
    protected void deallocate() {
        this.packets.forEach(ReferenceCountUtil::safeRelease);
        ReferenceCountUtil.safeRelease(this.uncompressed);
        ReferenceCountUtil.safeRelease(this.compressed);
        this.compressed = null;
        this.uncompressed = null;
        this.packets.clear();
        this.modified = false;
        this.algorithm = null;
        this.handle.recycle(this);
    }

    public void modify() {
        this.modified = true;
    }

    public void setCompressed(ByteBuf compressed) {
        if (this.compressed != null) {
            this.compressed.release();
        }

        this.compressed = compressed;
        if (compressed == null) {
            this.algorithm = null;
        }
    }

    public void setCompressed(ByteBuf compressed, CompressionAlgorithm algorithm) {
        if (this.compressed != null) {
            this.compressed.release();
        }

        this.compressed = compressed;
        this.algorithm = algorithm;
    }

    public void setUncompressed(ByteBuf uncompressed) {
        if (this.uncompressed != null) {
            this.uncompressed.release();
        }
        this.uncompressed = uncompressed;
    }

    public boolean skipQueue() {
        return this.skipQueue;
    }

    public BedrockBatchWrapper skipQueue(boolean skipQueue) {
        this.skipQueue = skipQueue;
        return this;
    }

    @Override
    public ReferenceCounted touch(Object o) {
        return this;
    }

    @Override
    public BedrockBatchWrapper retain() {
        return (BedrockBatchWrapper) super.retain();
    }

    @Override
    public BedrockBatchWrapper retain(int increment) {
        return (BedrockBatchWrapper) super.retain(increment);
    }
}
