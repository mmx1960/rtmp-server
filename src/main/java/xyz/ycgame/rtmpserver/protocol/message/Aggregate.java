package xyz.ycgame.rtmpserver.protocol.message;

import io.netty.buffer.ByteBuf;

public class Aggregate extends DataMessage {

    public Aggregate(RtmpHeader header, ByteBuf in) {
        super(header, in);
    }

    public Aggregate(int time, ByteBuf in) {
        super();
        header.setTime(time);
        data = in;
        header.setSize(data.readableBytes());
    }

    @Override
    MessageType getMessageType() {
        return MessageType.AGGREGATE;
    }

    @Override
    public boolean isConfig() {
        return false;
    }

}
