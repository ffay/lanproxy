package org.fengfei.lanproxy.protocol;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Arrays;

/**
 * 代理客户端与代理服务器消息交换协议
 *
 * @author fengfei
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProxyMessage implements Serializable {

    /**
     * 心跳消息
     */

    public transient static final byte TYPE_HEARTBEAT = 0x07;

    /**
     * 认证消息，检测clientKey是否正确
     */
    public transient static final byte C_TYPE_AUTH = 0x01;


    /**
     * 代理后端服务器建立连接消息
     */
    public transient static final byte TYPE_CONNECT = 0x03;

    /**
     * 代理后端服务器断开连接消息
     */
    public transient static final byte TYPE_DISCONNECT = 0x04;

    /**
     * 代理数据传输
     */
    public transient static final byte P_TYPE_TRANSFER = 0x05;

    /**
     * 用户与代理服务器以及代理客户端与真实服务器连接是否可写状态同步
     */
    public transient static final byte C_TYPE_WRITE_CONTROL = 0x06;

    /**
     * UDP Connect
     */
    public transient static final byte TYPE_UDP_CONNECT = 0x09;


    /**
     * UDP数据传输
     */
    public static final byte P_TYPE_TRANSFER_UDP = 0X08;

    /**
     * 消息类型
     */
    private byte type;

    /**
     * 消息流水号(数据长度)
     */
    private long serialNumber;

    /**
     * 消息命令请求信息
     */
    private String uri;

    /**
     * 消息传输数据
     */
    private byte[] data;


    @Override
    public String toString() {
        return "ProxyMessage [type=" + type + ", serialNumber=" + serialNumber + ", uri=" + uri + ", data=" + Arrays.toString(data) + "]";
    }


}
