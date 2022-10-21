package xyz.ycgame.rtmpserver.protocol.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import lombok.extern.slf4j.Slf4j;
import xyz.ycgame.rtmpserver.protocol.message.ChunkSize;
import xyz.ycgame.rtmpserver.protocol.message.MessageType;
import xyz.ycgame.rtmpserver.protocol.message.RtmpHeader;
import xyz.ycgame.rtmpserver.protocol.message.RtmpMessage;

import java.util.List;

@Slf4j
public class RtmpDecoderHandler extends ReplayingDecoder<RtmpDecoderHandler.DecoderState> {

    public enum DecoderState {
        /**
         *
         */
        GET_HEADER,
        /**
         *
         */
        GET_PAYLOAD
    }

    public RtmpDecoderHandler() {
        super(DecoderState.GET_HEADER);
    }

    private RtmpHeader header;

    private int channelId;

    private ByteBuf payload;

    private int chunkSize = 128;

    private final RtmpHeader[] incompleteHeaders = new RtmpHeader[RtmpHeader.MAX_CHANNEL_ID];

    private final ByteBuf[] incompletePayloads = new ByteBuf[RtmpHeader.MAX_CHANNEL_ID];

    private final RtmpHeader[] completedHeaders = new RtmpHeader[RtmpHeader.MAX_CHANNEL_ID];

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        switch (state()) {
            case GET_HEADER:
                header = new RtmpHeader(in, incompleteHeaders);
                channelId = header.getChannelId();
                // new chunk stream
                if (incompletePayloads[channelId] == null) {
                    incompleteHeaders[channelId] = header;
                    incompletePayloads[channelId] = ctx.channel().alloc().buffer(header.getSize());
                }
                payload = incompletePayloads[channelId];
                checkpoint(DecoderState.GET_PAYLOAD);
                break;
            case GET_PAYLOAD:
                final byte[] bytes = new byte[Math.min(payload.writableBytes(), chunkSize)];
                in.readBytes(bytes);
                payload.writeBytes(bytes);
                checkpoint(DecoderState.GET_HEADER);
                // more chunks remain
                if (payload.isWritable()) {
                    return;
                }
                incompletePayloads[channelId] = null;
                final RtmpHeader prevHeader = completedHeaders[channelId];
                if (!header.isLarge()) {
                    header.setTime(prevHeader.getTime() + header.getDeltaTime());
                }
                final RtmpMessage message = MessageType.decode(header, payload);
                if (log.isDebugEnabled()) {
                    log.debug("<< {}", message);
                }
                payload = null;
                if (header.isChunkSize()) {
                    final ChunkSize csMessage = (ChunkSize) message;
                    log.debug("decoder new chunk size: {}", csMessage);
                    chunkSize = csMessage.getChunkSize();
                }
                completedHeaders[channelId] = header;
                out.add(message);
                break;
            default:
                throw new RuntimeException("unexpected decoder state: " + state());
        }

    }

}
