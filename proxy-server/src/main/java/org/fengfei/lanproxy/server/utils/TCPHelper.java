package org.fengfei.lanproxy.server.utils;

import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TCPHelper {
    private static final Logger logger = LoggerFactory.getLogger(TCPHelper.class);

    // 简单判断是否是HTTP请求
    public static boolean isHttpRequest(ByteBuf buf) {
        logger.info("buf.readableBytes(): {}", buf.readableBytes());
        if (buf.readableBytes() < 4) {
            return false;
        }

        // 读取前4个字节进行判断，但不修改读索引
        byte[] firstBytes = new byte[4];
        buf.getBytes(buf.readerIndex(), firstBytes);
        String method = new String(firstBytes);
        logger.info("Method:{}", method);
        // 检查是否以常见的HTTP方法开头
        return method.startsWith("GET ") ||
                method.startsWith("POST") ||
                method.startsWith("PUT ") ||
                method.startsWith("HEAD") ||
                method.startsWith("DELE"); // DELETE
    }

    // 简单判断是否是HTTPS请求（SSL/TLS握手）
    public static boolean isSslRequest(ByteBuf buf) {
        if (buf.readableBytes() < 1) {
            return false;
        }

        // SSL/TLS握手消息以0x16开头
        return buf.getByte(buf.readerIndex()) == 0x16;
    }

    public static String getMethodName(ByteBuf buf) {
        // 读取前4个字节进行判断，但不修改读索引
        byte[] firstBytes = new byte[4];
        buf.getBytes(buf.readerIndex(), firstBytes);
        return new String(firstBytes);
    }
}
