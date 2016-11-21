package org.fengfei.lanproxy.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class ProxyMessageEncoder extends MessageToByteEncoder<ProxyMessage> {

    private static final int TYPE_SIZE = 1;

    private static final int SERIAL_NUMBER_SIZE = 8;

    private static final int URI_LENGTH_SIZE = 1;

    @Override
    protected void encode(ChannelHandlerContext ctx, ProxyMessage msg, ByteBuf out) throws Exception {
        int bodyLength = TYPE_SIZE + SERIAL_NUMBER_SIZE + URI_LENGTH_SIZE;
        byte[] uriBytes = null;
        if (msg.getUri() != null) {
            uriBytes = msg.getUri().getBytes();
            bodyLength += uriBytes.length;
        }

        if (msg.getData() != null) {
            bodyLength += msg.getData().length;
        }

        // write the total packet length but without length field's length.
        out.writeInt(bodyLength);

        out.writeByte(msg.getType());
        out.writeLong(msg.getSerialNumber());

        if (uriBytes != null) {
            out.writeByte((byte) uriBytes.length);
            out.writeBytes(uriBytes);
        } else {
            out.writeByte((byte) 0x00);
        }

        if (msg.getData() != null) {
            out.writeBytes(msg.getData());
        }
    }
}