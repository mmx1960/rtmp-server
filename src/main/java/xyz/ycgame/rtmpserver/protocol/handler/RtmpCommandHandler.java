package xyz.ycgame.rtmpserver.protocol.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import xyz.ycgame.rtmpserver.protocol.message.*;

/**
 * @author mmx1960
 * @since 2020/8/13
 */
@Slf4j
public class RtmpCommandHandler extends SimpleChannelInboundHandler<RtmpMessage> {

    private long bytesRead;

    private long bytesReadLastSent;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RtmpMessage msg) throws Exception {
        final Channel channel = ctx.channel();
        bytesRead += msg.getHeader().getSize();
        int bytesReadWindow = 150000;
        int bytesWrittenWindow = 150000;
        if ((bytesRead - bytesReadLastSent) > bytesReadWindow) {
            BytesRead ack = new BytesRead(bytesRead);
            channel.write(ack);
            bytesReadLastSent = bytesRead;
        }
        switch (msg.getHeader().getMessageType()) {
            case CHUNK_SIZE:
                break;
            case SHARED_OBJECT_AMF0:
                //todo 解析sharedObject
                break;
            case COMMAND_AMF0:
            case COMMAND_AMF3:
                Command cmd = (Command) msg;
                //转成同用的数据格式
                ctx.fireChannelRead(cmd);
                break;
            case WINDOW_ACK_SIZE:
                WindowAckSize was = (WindowAckSize) msg;
                if (was.getValue() != bytesReadWindow) {
                    channel.write(SetPeerBw.dynamic(bytesReadWindow));
                }
                break;
            case SET_PEER_BW:
                SetPeerBw spb = (SetPeerBw) msg;
                if (spb.getValue() != bytesWrittenWindow) {
                    channel.write(new WindowAckSize(bytesWrittenWindow));
                }
                break;
            default:
                log.warn("ignoring message: {}", msg);
        }
    }
}
