package xyz.ycgame.rtmpserver.protocol.message;

import io.netty.buffer.ByteBuf;
import xyz.ycgame.rtmpserver.protocol.amf.Amf0Value;

public class SharedObject extends AbstractMessage{

    public SharedObject(RtmpHeader header, ByteBuf in) {
        super(header, in);
    }

    @Override
    public ByteBuf encode() {
        return null;
    }

    @Override
    public void decode(ByteBuf in) {
        String name = Amf0Value.getString(in);
        int version = in.readInt();
        boolean persistent = in.readInt() == 2;
        in.skipBytes(4);
        //TODO 解析SharedObject
    }

    @Override
    MessageType getMessageType() {
        return MessageType.SHARED_OBJECT_AMF0;
    }
}
