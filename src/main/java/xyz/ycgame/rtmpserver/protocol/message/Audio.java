package xyz.ycgame.rtmpserver.protocol.message;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class Audio extends DataMessage {

    @Override
    public boolean isConfig() { // TODO now hard coded for mp4a
        return data.readableBytes() > 3 && data.getInt(0) == 0xaf001310;
    }

    public Audio(final RtmpHeader header, final ByteBuf in) {
        super(header, in);
    }

    public Audio(final byte[] ... bytes) {
        super(bytes);
    }

    public Audio(final int time, final byte[] prefix, final byte[] audioData) {
        header.setTime(time);
        data = Unpooled.wrappedBuffer(prefix, audioData);
        header.setSize(data.readableBytes());
    }

    public Audio(final int time, final ByteBuf in) {
        super(time, in);
    }
    
    public static Audio empty() {
        Audio empty = new Audio();
        empty.data = Unpooled.EMPTY_BUFFER;
        return empty;
    }

    @Override
    MessageType getMessageType() {
        return MessageType.AUDIO;
    }

}
