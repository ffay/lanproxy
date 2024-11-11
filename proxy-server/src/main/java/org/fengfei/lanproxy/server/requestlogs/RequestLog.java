package org.fengfei.lanproxy.server.requestlogs;

import java.util.Date;

//定义日志对象
public class RequestLog {
    private String ip;
    private int port;
    private Date time;
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

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public String getRequestInfo() {
        return requestInfo;
    }

    public Date getTime() {
        return time;
    }
}