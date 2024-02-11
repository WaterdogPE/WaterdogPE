package dev.waterdog.waterdogpe.network.connection.codec.server;

import dev.waterdog.waterdogpe.network.NetworkMetrics;
import dev.waterdog.waterdogpe.network.connection.codec.batch.BatchFlags;
import dev.waterdog.waterdogpe.network.connection.peer.BedrockServerSession;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.internal.PlatformDependent;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.protocol.bedrock.netty.BedrockBatchWrapper;

import java.util.Queue;

@Log4j2
public class PacketQueueHandler extends ChannelDuplexHandler {
    public static final String NAME = "packet-queue-handler";
    private static final int MAX_BATCHES = 256;
    private static final int MAX_PACKETS = 8000;

    private final BedrockServerSession session;

    private int packetCounter = 0;
    private final Queue<BedrockBatchWrapper> queue = PlatformDependent.newMpscQueue(MAX_BATCHES);

    private volatile boolean finished;

    public PacketQueueHandler(BedrockServerSession session) {
        this.session = session;
    }

    private void finish(ChannelHandlerContext ctx, boolean send) {
        if (this.finished) {
            return;
        }
        this.finished = true;

        if (ctx.pipeline().get(NAME) == this) {
            ctx.pipeline().remove(this);
        }

        BedrockBatchWrapper batch;
        while ((batch = this.queue.poll()) != null) {
            if (send) {
                ctx.write(batch);
            } else {
                batch.release();
            }
        }

        if (send) {
            ctx.flush();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        this.finish(ctx, false);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        this.finish(ctx, ctx.channel().isActive());
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (this.finished || !(msg instanceof BedrockBatchWrapper batch) || batch.hasFlag(BatchFlags.SKIP_QUEUE)) {
            ctx.write(msg, promise);
            return;
        }

        if (this.queue.offer(batch) && this.packetCounter < MAX_PACKETS) {
            this.packetCounter += batch.getPackets().size();
        } else {
            log.warn("[{}] has reached maximum transfer queue capacity: batches={} packets={}", this.session.getSocketAddress(), this.queue.size(), this.packetCounter);
            this.finish(ctx, false);
            this.session.disconnect("Transfer queue got too large");

            NetworkMetrics metrics = ctx.channel().attr(NetworkMetrics.ATTRIBUTE).get();
            if (metrics != null) {
                metrics.packetQueueTooLarge();
            }
        }
    }
}
