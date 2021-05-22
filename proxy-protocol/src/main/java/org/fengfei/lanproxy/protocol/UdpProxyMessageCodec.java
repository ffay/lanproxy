package org.fengfei.lanproxy.protocol;

import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

/**
 * Udp数据编解码
 */
public class UdpProxyMessageCodec {

    static Schema<ProxyMessage> schema = RuntimeSchema.getSchema(ProxyMessage.class);


    public static byte[] encode(ProxyMessage proxyMessage) {

        LinkedBuffer buffer = LinkedBuffer.allocate(512);

        // ser
        final byte[] protostuff;
        try {
            protostuff = ProtostuffIOUtil.toByteArray(proxyMessage, schema, buffer);
        } finally {
            buffer.clear();
        }
        return protostuff;
    }

    public static ProxyMessage decode(byte[] bytes) {
        ProxyMessage proxyMessage = schema.newMessage();
        ProtostuffIOUtil.mergeFrom(bytes, proxyMessage, schema);
        return proxyMessage;
    }

    public static void main(String[] args) {
        ProxyMessage proxyMessage = new ProxyMessage(ProxyMessage.P_TYPE_TRANSFER, 12, "dd", new byte[]{12, 33});
        byte[] encode = encode(proxyMessage);

        ProxyMessage decode = decode(encode);
        System.out.println(decode);
    }
}
