package cn.dxbtech.portbridge.server.config.web;

import io.netty.handler.codec.http.HttpResponseStatus;

import java.io.Serializable;

public class ResponseInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private HttpResponseStatus status;

    private String message;

    private Object data;

    private ResponseInfo(HttpResponseStatus status, String message, Object data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    private ResponseInfo(HttpResponseStatus code, Object data) {
        this(code, null, data);
    }

    private ResponseInfo(Object data) {
        this(HttpResponseStatus.OK, data);
    }

    public static ResponseInfo build(HttpResponseStatus code, String message) {
        return new ResponseInfo(code, message);
    }

    public static ResponseInfo build(Object data) {
        return new ResponseInfo(data);
    }

    public HttpResponseStatus getStatus() {
        return status;
    }

    public void setStatus(HttpResponseStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

}