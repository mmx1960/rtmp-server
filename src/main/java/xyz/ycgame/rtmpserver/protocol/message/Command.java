package xyz.ycgame.rtmpserver.protocol.message;

import io.netty.buffer.ByteBuf;
import xyz.ycgame.rtmpserver.protocol.amf.Amf0Object;

import java.util.Arrays;

/**
 * @author mmx1960
 */
public abstract class Command extends AbstractMessage {

    protected String name;
    protected int transactionId;
    protected Amf0Object object;
    protected Object[] args;

    public Command(RtmpHeader header, ByteBuf in) {
        super(header, in);
    }

    public Command(int transactionId, String name, Amf0Object object, Object... args) {
        this.transactionId = transactionId;
        this.name = name;
        this.object = object;
        this.args = args;
    }

    public Command(String name, Amf0Object object, Object... args) {
        this(0, name, object, args);
    }

    public Amf0Object getObject() {
        return object;
    }

    public Object getArg(int index) {
        return args[index];
    }

    public int getArgCount() {
        if (args == null) {
            return 0;
        }
        return args.length;
    }

    public Object[] getArgs() {
        return args;
    }


    public static Command data(int transactionId, String name, Amf0Object amf0Object, Object[] args) {
        return new CommandAmf0(transactionId, name, amf0Object, args);
    }

    //==========================================================================

    public String getName() {
        return name;
    }

    public int getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        sb.append("name: ").append(name);
        sb.append(", transactionId: ").append(transactionId);
        sb.append(", object: ").append(object);
        sb.append(", args: ").append(Arrays.toString(args));
        return sb.toString();
    }

}
