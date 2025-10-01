package org.fengfei.lanproxy.server.utils;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class PacketParser {
    private static final Logger logger = LoggerFactory.getLogger(PacketParser.class);

    public static class PacketInfo {
        private String protocol;
        private Map<String, String> headers = new HashMap<>();
        private String rawData;
        private Map<String, Object> attributes = new HashMap<>();

        // Getters and setters
        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }

        public String getRawData() {
            return rawData;
        }

        public void setRawData(String rawData) {
            this.rawData = rawData;
        }

        public Map<String, Object> getAttributes() {
            return attributes;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(protocol).append(" - ");

            if ("SSH".equals(protocol)) {
                sb.append("Type: ").append(attributes.get("messageType"));
                if (attributes.containsKey("version")) {
                    sb.append(", Version: ").append(attributes.get("version"));
                }
                if (attributes.containsKey("software")) {
                    sb.append(", Client: ").append(attributes.get("software"));
                }
            } else if ("PostgreSQL".equals(protocol)) {
                sb.append("Type: ").append(attributes.get("messageType"));
                if ("StartupMessage".equals(attributes.get("messageType"))) {
                    sb.append(", Version: ").append(attributes.get("protocolVersion"));
                    @SuppressWarnings("unchecked")
                    Map<String, String> params = (Map<String, String>) attributes.get("parameters");
                    if (params != null) {
                        sb.append("\nParameters:");
                        params.forEach((k, v) -> sb.append("\n  ").append(k).append(": ").append(v));
                    }
                }
            } else if (protocol.startsWith("HTTP")) {
                sb.append(attributes.get("method")).append(" ")
                        .append(attributes.get("path")).append("\n");
                if (headers.containsKey("User-Agent")) {
                    sb.append("User-Agent: ").append(headers.get("User-Agent")).append("\n");
                }
                if (headers.containsKey("Content-Type")) {
                    sb.append("Content-Type: ").append(headers.get("Content-Type"));
                }
            } else {
                sb.append("Size: ").append(attributes.get("size")).append(" bytes");
                if (attributes.containsKey("preview")) {
                    sb.append("\nPreview: ").append(attributes.get("preview"));
                }
            }
            return sb.toString();
        }
    }

    public static PacketInfo parsePacket(ByteBuf buf) {
        PacketInfo info = new PacketInfo();
        if (buf == null || buf.readableBytes() == 0) {
            info.setProtocol("UNKNOWN");
            return info;
        }

        // 检测协议
        if (isSSHPacket(buf)) {
            parseSSHPacket(buf, info);
        } else if (isHttpRequest(buf)) {
            parseHttpRequest(buf, info);
        } else if (isSslRequest(buf)) {
            parseSslRequest(buf, info);
        } else if (isPostgreSQLPacket(buf)) {
            parsePostgreSQLPacket(buf, info);
        } else if (isDnsPacket(buf)) {
            parseDnsPacket(buf, info);
        } else {
            parseGenericPacket(buf, info);
        }

        return info;
    }

    private static void parseHttpRequest(ByteBuf buf, PacketInfo info) {
        String content = buf.toString(StandardCharsets.UTF_8);
        String[] lines = content.split("\r\n");

        // 解析请求行
        String[] requestLine = lines[0].split(" ");
        info.setProtocol("HTTP");
        info.getAttributes().put("method", requestLine[0]);
        info.getAttributes().put("path", requestLine[1]);
        info.getAttributes().put("version", requestLine[2]);

        // 解析请求头
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line.isEmpty()) break;

            int colonIdx = line.indexOf(':');
            if (colonIdx > 0) {
                String key = line.substring(0, colonIdx).trim();
                String value = line.substring(colonIdx + 1).trim();
                info.getHeaders().put(key, value);
            }
        }
    }

    private static void parseSslRequest(ByteBuf buf, PacketInfo info) {
        info.setProtocol("HTTPS");
        info.getAttributes().put("size", buf.readableBytes());
        info.getAttributes().put("sslVersion", buf.getByte(buf.readerIndex() + 1) & 0xFF);
    }

    private static void parseDnsPacket(ByteBuf buf, PacketInfo info) {
        info.setProtocol("DNS");
        info.getAttributes().put("size", buf.readableBytes());
        // 解析DNS包头
        if (buf.readableBytes() >= 12) {
            info.getAttributes().put("transactionId", buf.getShort(buf.readerIndex()));
            info.getAttributes().put("flags", buf.getShort(buf.readerIndex() + 2));
        }
    }

    private static void parseGenericPacket(ByteBuf buf, PacketInfo info) {
        info.setProtocol("TCP/UDP");
        info.getAttributes().put("size", buf.readableBytes());

        // 生成16进制预览
        int previewSize = Math.min(16, buf.readableBytes());
        byte[] preview = new byte[previewSize];
        buf.getBytes(buf.readerIndex(), preview);
        StringBuilder hexPreview = new StringBuilder();
        StringBuilder asciiPreview = new StringBuilder();

        for (byte b : preview) {
            hexPreview.append(String.format("%02X ", b));
            asciiPreview.append(isPrintable(b) ? (char) b : '.');
        }

        info.getAttributes().put("preview",
                String.format("HEX: %s | ASCII: %s", hexPreview, asciiPreview));
    }

    private static boolean isPostgreSQLPacket(ByteBuf buf) {
        if (buf.readableBytes() < 8) return false;

        // PostgreSQL 启动消息的长度至少为8字节
        int messageLength = buf.getInt(buf.readerIndex());
        if (messageLength < 8) return false;

        // 检查协议版本号(196608 = 3.0)
        int protocolVersion = buf.getInt(buf.readerIndex() + 4);
        return protocolVersion == 196608 || protocolVersion == 80877103; // 80877103是SSLRequest
    }

    private static void parsePostgreSQLPacket(ByteBuf buf, PacketInfo info) {
        info.setProtocol("PostgreSQL");
        int messageLength = buf.getInt(buf.readerIndex());
        int protocolVersion = buf.getInt(buf.readerIndex() + 4);

        info.getAttributes().put("messageLength", messageLength);

        if (protocolVersion == 80877103) {
            info.getAttributes().put("messageType", "SSLRequest");
        } else {
            info.getAttributes().put("messageType", "StartupMessage");
            info.getAttributes().put("protocolVersion", "3.0");

            // 尝试解析参数
            if (buf.readableBytes() > 8) {
                Map<String, String> params = new HashMap<>();
                int offset = 8;
                StringBuilder paramBuilder = new StringBuilder();

                while (offset < messageLength - 1) {
                    paramBuilder.setLength(0);
                    while (buf.getByte(buf.readerIndex() + offset) != 0) {
                        paramBuilder.append((char) buf.getByte(buf.readerIndex() + offset));
                        offset++;
                    }
                    String key = paramBuilder.toString();
                    offset++; // 跳过null字节

                    paramBuilder.setLength(0);
                    while (offset < messageLength - 1 && buf.getByte(buf.readerIndex() + offset) != 0) {
                        paramBuilder.append((char) buf.getByte(buf.readerIndex() + offset));
                        offset++;
                    }
                    String value = paramBuilder.toString();
                    offset++; // 跳过null字节

                    if (!key.isEmpty()) {
                        params.put(key, value);
                    }
                }

                info.getAttributes().put("parameters", params);
            }
        }
    }

    private static boolean isSSHPacket(ByteBuf buf) {
        if (buf.readableBytes() < 4) return false;
        
        // SSH协议以"SSH-"开头
        byte[] header = new byte[4];
        buf.getBytes(buf.readerIndex(), header);
        return new String(header).equals("SSH-");
    }

    private static void parseSSHPacket(ByteBuf buf, PacketInfo info) {
        info.setProtocol("SSH");
        
        // 读取SSH版本信息
        String content = buf.toString(StandardCharsets.UTF_8);
        String[] parts = content.split("-", 3);
        if (parts.length >= 3) {
            // 格式通常是: SSH-2.0-OpenSSH_8.1
            String version = parts[1];
            String software = parts[2].split("\\r?\\n")[0];
            
            info.getAttributes().put("version", version);
            info.getAttributes().put("software", software);
            info.getAttributes().put("messageType", "Version Exchange");
        }
        
        info.getAttributes().put("size", buf.readableBytes());
    }

    // 其他辅助方法...
    private static boolean isHttpRequest(ByteBuf buf) {
        if (buf.readableBytes() < 4) return false;
        byte[] firstBytes = new byte[4];
        buf.getBytes(buf.readerIndex(), firstBytes);
        String method = new String(firstBytes, StandardCharsets.UTF_8);
        return method.startsWith("GET ") || method.startsWith("POST") ||
                method.startsWith("PUT ") || method.startsWith("HEAD") ||
                method.startsWith("DELE");
    }

    private static boolean isSslRequest(ByteBuf buf) {
        return buf.readableBytes() >= 5 &&
                buf.getByte(buf.readerIndex()) == 0x16;
    }

    private static boolean isDnsPacket(ByteBuf buf) {
        return buf.readableBytes() >= 12 &&
                (buf.getShort(buf.readerIndex() + 2) & 0x8000) != 0;
    }

    private static boolean isPrintable(byte b) {
        return b >= 32 && b < 127;
    }
} 