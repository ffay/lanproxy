package org.fengfei.lanproxy.server.utils;

import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TCPHelper {
    private static final Logger logger = LoggerFactory.getLogger(TCPHelper.class);

    // 简单判断是否是HTTP请求
    public static boolean isHttpRequest(ByteBuf buf) {
        if (buf.readableBytes() < 4) {
            return false;
        }

        // 读取前4个字节进行判断，但不修改读索引
        byte[] firstBytes = new byte[4];
        buf.getBytes(buf.readerIndex(), firstBytes);
        String method = new String(firstBytes);
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

    // 解析TCP/UDP请求包
    public static String parsePacket(ByteBuf buf) {
        if (buf == null || buf.readableBytes() == 0) {
            return "Empty packet";
        }

        StringBuilder info = new StringBuilder();
        
        // 检查是否是HTTP/HTTPS请求
        if (isHttpRequest(buf)) {
            String method = getMethodName(buf);
            String protocol = isSslRequest(buf) ? "HTTPS" : "HTTP";
            info.append(method).append(" ").append(protocol);
        } else {
            // 尝试解析为普通TCP/UDP包
            info.append("TCP/UDP Packet - Size: ").append(buf.readableBytes()).append(" bytes");
            
            // 读取前16个字节作为预览（如果有）
            int previewSize = Math.min(16, buf.readableBytes());
            byte[] preview = new byte[previewSize];
            buf.getBytes(buf.readerIndex(), preview);
            
            // 添加十六进制预览
            info.append(" - Preview(hex): ");
            for (byte b : preview) {
                info.append(String.format("%02X ", b));
            }
            
            // 尝试ASCII预览
            info.append(" - ASCII: ");
            for (byte b : preview) {
                info.append(isPrintable(b) ? (char)b : '.');
            }
        }
        
        return info.toString();
    }
    
    private static boolean isPrintable(byte b) {
        return b >= 32 && b < 127;
    }
}
