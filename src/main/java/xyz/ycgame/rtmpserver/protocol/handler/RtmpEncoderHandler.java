package xyz.ycgame.rtmpserver.protocol.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import lombok.extern.slf4j.Slf4j;
import xyz.ycgame.rtmpserver.protocol.amf.Amf0Object;
import xyz.ycgame.rtmpserver.protocol.message.*;

import static xyz.ycgame.rtmpserver.protocol.message.AbstractMessage.pair;

/**
 * @author mmx1960
 */
@Slf4j
public class RtmpEncoderHandler extends ChannelOutboundHandlerAdapter {
    private static final Amf0Object AMF_OBJ = AbstractMessage.object(pair("fmsVer", "FMS/3,5,1,516"),
            pair("capabilities", 31),
            pair("mode", 1));

    private int chunkSize = 128;

    private RtmpHeader[] channelPrevHeaders = new RtmpHeader[RtmpHeader.MAX_CHANNEL_ID];

    private void clearPrevHeaders() {
        log.debug("clearing prev stream headers");
        channelPrevHeaders = new RtmpHeader[RtmpHeader.MAX_CHANNEL_ID];
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {

        ByteBuf encode = encode(convertToRtmpMessage(msg), ctx.channel());
        ctx.writeAndFlush(encode);
        encode.clear();
    }

    private RtmpMessage convertToRtmpMessage(Object msg) {
        if (msg == null) {
            return null;
        }
        if (msg instanceof RtmpMessage) {
            return (RtmpMessage) msg;
        }
        return null;
    }

    public ByteBuf encode(final RtmpMessage message, Channel channel) {
        final ByteBuf in = message.encode();
        final RtmpHeader header = message.getHeader();
        if (header.isChunkSize()) {
            final ChunkSize csMessage = (ChunkSize) message;
            log.debug("encoder new chunk size: {}", csMessage);
            chunkSize = csMessage.getChunkSize();
        } else if (header.isControl()) {
            final Control control = (Control) message;
            if (control.getType() == Control.Type.STREAM_BEGIN) {
                clearPrevHeaders();
            }
        }
        final int channelId = header.getChannelId();
        header.setSize(in.readableBytes());
        final RtmpHeader prevHeader = channelPrevHeaders[channelId];
        // 1. first stream message is always large
        // 2. all control messages always large
        // 3. if time is zero, always large
        if (prevHeader != null
                && header.getStreamId() > 0
                && header.getTime() > 0) {
            if (header.getSize() == prevHeader.getSize()) {
                header.setHeaderType(RtmpHeader.Type.SMALL);
            } else {
                header.setHeaderType(RtmpHeader.Type.MEDIUM);
            }
            final int deltaTime = header.getTime() - prevHeader.getTime();
            if (deltaTime < 0) {
                log.warn("negative time: {}", header);
                header.setDeltaTime(0);
            } else {
                header.setDeltaTime(deltaTime);
            }
        } else {
            // otherwise force to LARGE
            header.setHeaderType(RtmpHeader.Type.LARGE);
        }
        channelPrevHeaders[channelId] = header;
        if (log.isDebugEnabled()) {
            log.debug(">> {}", message);
        }
        final ByteBuf out = channel.alloc()
                .buffer(RtmpHeader.MAX_ENCODED_SIZE + header.getSize() + header.getSize() / chunkSize);
        boolean first = true;
        while (in.isReadable()) {
            final int size = Math.min(chunkSize, in.readableBytes());
            if (first) {
                header.encode(out);
                first = false;
            } else {
                out.writeBytes(header.getTinyHeader());
            }
            in.readBytes(out, size);
        }
        return out;
    }

}
