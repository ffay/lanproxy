package org.fengfei.lanproxy.server.requestlogs;

import io.netty.buffer.ByteBuf;

import java.util.Date;

//定义日志对象
public class RequestLog {
    private String ip;
    private int port;
    private Date time;
    private String userId;
    private ByteBuf data;
    private String requestInfo;

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public void setRequestInfo(String requestInfo) {
        this.requestInfo = requestInfo;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public String getRequestInfo() {
        return requestInfo;
    }

    public String getUserId() {
        return userId;
    }

    public Date getTime() {
        return time;
    }

    public ByteBuf getData() {
        return data;
    }

    public void setData(ByteBuf data) {
        this.data = data;
    }
}