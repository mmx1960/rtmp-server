package xyz.ycgame.rtmpserver.protocol.message;

import io.netty.buffer.ByteBuf;
import xyz.ycgame.rtmpserver.protocol.amf.IntValue;
import xyz.ycgame.rtmpserver.util.ValueToEnum;

public enum MessageType implements IntValue {
    CHUNK_SIZE(0x01),
    ABORT(0x02),
    BYTES_READ(0x03),
    CONTROL(0x04),
    WINDOW_ACK_SIZE(0x05),
    SET_PEER_BW(0x06),
    // unknown 0x07
    AUDIO(0x08),
    VIDEO(0x09),
    // unknown 0x0A - 0x0E
    METADATA_AMF3(0x0F),
    SHARED_OBJECT_AMF3(0x10),
    COMMAND_AMF3(0x11),
    METADATA_AMF0(0x12),
    SHARED_OBJECT_AMF0(0x13),
    COMMAND_AMF0(0x14),
    AGGREGATE(0x16);

    private final int value;

    private MessageType(final int value) {
        this.value = value;
    }

    @Override
    public int intValue() {
        return value;
    }

    public int getDefaultChannelId() {
        switch (this) {
            case CHUNK_SIZE:
            case CONTROL:
            case ABORT:
            case BYTES_READ:
            case WINDOW_ACK_SIZE:
            case SET_PEER_BW:
                return 2;
            case COMMAND_AMF0:
            case COMMAND_AMF3: // TODO verify
                return 3;
            case METADATA_AMF0:
            case METADATA_AMF3: // TODO verify
            case AUDIO:
            case VIDEO:
            case AGGREGATE:
            default: // TODO verify
                return 5;
        }
    }

    public static RtmpMessage decode(final RtmpHeader header, final ByteBuf in) {
        switch (header.getMessageType()) {
            case ABORT:
                return new Abort(header, in);
            case BYTES_READ:
                return new BytesRead(header, in);
            case CHUNK_SIZE:
                return new ChunkSize(header, in);
            case COMMAND_AMF0:
                return new CommandAmf0(header, in);
            case CONTROL:
                return new Control(header, in);
            case WINDOW_ACK_SIZE:
                return new WindowAckSize(header, in);
            case SET_PEER_BW:
                return new SetPeerBw(header, in);
            case AUDIO:
                return new Audio(header, in);
            case AGGREGATE:
                return new Aggregate(header, in);
            case SHARED_OBJECT_AMF0:
                return new SharedObject(header, in);
            default:
                throw new RuntimeException("unable to create message for: " + header);
        }
    }

    private static final ValueToEnum<MessageType> converter = new ValueToEnum<MessageType>(MessageType.values());

    public static MessageType valueToEnum(final int value) {
        return converter.valueToEnum(value);
    }

}
