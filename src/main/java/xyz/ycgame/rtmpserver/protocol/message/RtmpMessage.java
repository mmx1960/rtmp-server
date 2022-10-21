package xyz.ycgame.rtmpserver.protocol.message;

import io.netty.buffer.ByteBuf;

/**
 * @author mmx1960
 */
public interface RtmpMessage {

    RtmpHeader getHeader();

    ByteBuf encode();

    void decode(ByteBuf in);

}
