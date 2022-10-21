package xyz.ycgame.rtmpserver.protocol.message;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class WindowAckSize extends AbstractMessage {
    
    private int value;
    
    public WindowAckSize(RtmpHeader header, ByteBuf in) {
        super(header, in);
    }
    
    public WindowAckSize(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    MessageType getMessageType() {
        return MessageType.WINDOW_ACK_SIZE;
    }

    @Override
    public ByteBuf encode() {
        ByteBuf out = Unpooled.buffer(4);
        out.writeInt(value);
        return out;
    }

    @Override
    public void decode(ByteBuf in) {
        value = in.readInt();
    }

    @Override
    public String toString() {
        return super.toString() + value;
    }

}
