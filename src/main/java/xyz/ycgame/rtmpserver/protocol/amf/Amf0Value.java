package xyz.ycgame.rtmpserver.protocol.amf;

import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;
import xyz.ycgame.rtmpserver.util.JsonUtils;
import xyz.ycgame.rtmpserver.util.ValueToEnum;

import java.io.UnsupportedEncodingException;
import java.util.*;

import static xyz.ycgame.rtmpserver.protocol.amf.Amf0Value.Type.*;

@Slf4j
public class Amf0Value {

    public static enum Type implements IntValue {
        /**
         * 
         */
        NUMBER(0x00),
        /**
         * 
         */
        BOOLEAN(0x01),
        /**
         * 
         */
        STRING(0x02),
        /**
         *
         */
        OBJECT(0x03),
        /**
         *
         */
        NULL(0x05),
        /**
         *
         */
        UNDEFINED(0x06),
        /**
         *
         */
        REFERENCE(0x07),
        /**
         *
         */
        MAP(0x08),
        /**
         *
         */
        OBJECT_END(0x09),
        /**
         *
         */
        ARRAY(0x0A),
        /**
         *
         */
        LIST(0x0A),
        /**
         *
         */
        DATE(0x0B),
        /**
         *
         */
        LONG_STRING(0x0C),
        /**
         *
         */
        UNSUPPORTED(0x0D),
        /**
         *
         */
        RECORDSET(0x0E),
        /**
         *
         */
        XML_DOCUMENT(0x0F),
        /**
         *
         */
        TYPED_OBJECT(0x10);
        private final int value;

        private Type(int value) {
            this.value = value;
        }

        @Override
        public int intValue() {
            return value;
        }

        private static final ValueToEnum<Type> converter = new ValueToEnum<Type>(
                Type.values());

        public static Type valueToEnum(final int value) {
            return converter.valueToEnum(value);
        }

        private static Type getType(final Object value) {
            if (value == null) {
                return NULL;
            } else if (value instanceof String) {
                return STRING;
            } else if (value instanceof Number) {
                return NUMBER;
            } else if (value instanceof Boolean) {
                return BOOLEAN;
            } else if (value instanceof Amf0Object) {
                Amf0Object obj = (Amf0Object) value;
                if (obj.containsKey("classname")) {
                    return TYPED_OBJECT;
                } else {
                    return OBJECT;
                }
            } else if (value instanceof Map) {
                return OBJECT;
            } else if (value instanceof List) {
                return LIST;
            } else if (value instanceof Object[]) {
                return ARRAY;
            } else if (value instanceof Date) {
                return DATE;
            }else if (value instanceof Undefined){
                return UNDEFINED;
            } else {
                return OBJECT;
            }
        }
    }

    private static final byte BOOLEAN_TRUE = 0x01;
    private static final byte BOOLEAN_FALSE = 0x00;
    private static final byte[] OBJECT_END_MARKER = new byte[]{0x00, 0x00,
            0x09};

    @SuppressWarnings("unchecked")
    public static void encode(final ByteBuf out, final Object value) {
        final Type type = Type.getType(value);
        if (log.isDebugEnabled()) {
            log.debug(">> " + toString(type, value));
        }
        out.writeByte((byte) type.value);
        switch (type) {
            case NUMBER:
                if (value instanceof Double) {
                    out.writeLong(Double.doubleToLongBits((Double) value));
                } else { // this coverts int also
                    out.writeLong(Double.doubleToLongBits(Double.parseDouble(value
                            .toString())));
                }
                return;
            case BOOLEAN:
                out.writeByte((Boolean) value ? BOOLEAN_TRUE : BOOLEAN_FALSE);
                return;
            case STRING:
                encodeString(out, (String) value);
                return;
            case NULL:
            case UNDEFINED:
                return;
            case MAP:
                // no break; remaining processing same as OBJECT
            case OBJECT:
                if (value instanceof Map){
                    encodeObject(out, value);
                }else {
                    encodeObject(out, JsonUtils.objectToMap(value));
                }
                return;
            case LIST:
               final List list = (List) value;
                out.writeInt(list.size());
                for (Object o : list) {
                    encode(out, o);
                }
                return;
            case ARRAY:
                final Object[] array = (Object[]) value;
                out.writeInt(array.length);
                for (Object o : array) {
                    encode(out, o);
                }
                return;
            case DATE:
                final long time = ((Date) value).getTime();
                out.writeLong(Double.doubleToLongBits(time));
                out.writeShort((short) 0);
                return;
            case TYPED_OBJECT:
                final Map<String, Object> map = (Map<String, Object>) value;
                encodeString(out, (String) map.remove("classname"));
                encodeObject(out, value);
                return;
            default:
                // ignoring other types client doesn't require for now
                throw new RuntimeException("unexpected type: " + type);
        }
    }

    private static String decodeString(final ByteBuf in) {
        final short size = in.readShort();
        final byte[] bytes = new byte[size];
        in.readBytes(bytes);
        try {
            return new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static void encodeString(final ByteBuf out, final String value) {
        byte[] bytes;
        try {
            bytes = value.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        out.writeShort((short) bytes.length);
        out.writeBytes(bytes);
    }

    @SuppressWarnings("unchecked")
    private static void encodeObject(final ByteBuf out, final Object value) {
        final Map<String, Object> map = (Map<String, Object>) value;
        for (final Map.Entry<String, Object> entry : map.entrySet()) {
            encodeString(out, entry.getKey());
            encode(out, entry.getValue());
        }
        out.writeBytes(OBJECT_END_MARKER);
    }

    public static void encode(final ByteBuf out, final Object... values) {
        for (final Object value : values) {
            encode(out, value);
        }
    }

    public static Object decode(final ByteBuf in) {
        final Type type = Type.valueToEnum(in.readByte());
        final Object value = decode(in, type);
        if (log.isDebugEnabled()) {
            log.debug("<< " + toString(type, value));
        }
        return value;
    }
    public static String getString(final ByteBuf in){
        return (String) decode(in,STRING);
    }

    private static Object decode(final ByteBuf in, final Type type) {
        switch (type) {
            case NUMBER:
                return in.readDouble();
            case BOOLEAN:
                return in.readByte() == BOOLEAN_TRUE;
            case STRING:
                return decodeString(in);
            case ARRAY:
                final int arraySize = in.readInt();
                final Object[] array = new Object[arraySize];
                for (int i = 0; i < arraySize; i++) {
                    array[i] = decode(in);
                }
                return array;
            case MAP:
            case OBJECT:
                final int count;
                final Map<String, Object> map;
                if (type == MAP) {
                    count = in.readInt(); // should always be 0
                    map = new LinkedHashMap<String, Object>();
                    if (count > 0 && log.isDebugEnabled()) {
                        log.debug("non-zero size for MAP type: {}", count);
                    }
                } else {
                    count = 0;
                    map = new Amf0Object();
                }
                int i = 0;
                final byte[] endMarker = new byte[3];
                while (in.isReadable()) {
                    in.getBytes(in.readerIndex(), endMarker);
                    if (Arrays.equals(endMarker, OBJECT_END_MARKER)) {
                        in.skipBytes(3);
                        if (log.isDebugEnabled()) {
                            log.debug("end MAP / OBJECT, found object end marker [000009]");
                        }
                        break;
                    }
                    if (count > 0 && i++ == count) {
                        if (log.isDebugEnabled()) {
                            log.debug(
                                    "stopping map decode after reaching count: {}",
                                    count);
                        }
                        break;
                    }
                    map.put(decodeString(in), decode(in));
                }
                return map;
            case DATE:
                final long dateValue = in.readLong();
                in.readShort(); // consume the timezone
                return new Date((long) Double.longBitsToDouble(dateValue));
            case LONG_STRING:
                final int stringSize = in.readInt();
                final byte[] bytes = new byte[stringSize];
                in.readBytes(bytes);
                return new String(bytes); // TODO UTF-8 ?
            case NULL:
            case UNDEFINED:
            case UNSUPPORTED:
                return null;
            case TYPED_OBJECT:
                String classname = decodeString(in);
                Amf0Object object = (Amf0Object) decode(in, OBJECT);
                object.put("classname", classname);
                return object;
            default:
                throw new RuntimeException("unexpected type: " + type);
        }
    }

    private static String toString(final Type type, final Object value) {
        StringBuilder sb = new StringBuilder();
        sb.append('[').append(type).append(" ");
        if (type == Type.ARRAY) {
            sb.append(Arrays.toString((Object[]) value));
        } else {
            sb.append(value);
        }
        sb.append(']');
        return sb.toString();
    }
}
