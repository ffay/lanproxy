package org.fengfei.lanproxy.server.config.web;

import java.io.Serializable;

public class ResponseInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int CODE_OK = 20000;

    public static final int CODE_INVILID_PARAMS = 40000;

    public static final int CODE_UNAUTHORIZED = 40100;

    public static final int CODE_API_NOT_FOUND = 40400;

    public static final int CODE_SYSTEM_ERROR = 50000;

    private int code;

    private String message;

    private Object data;

    public static ResponseInfo build(int code, String message, Object data) {
        return new ResponseInfo(code, message, data);
    }

    public static ResponseInfo build(int code, String message) {
        return new ResponseInfo(code, message);
    }

    public static ResponseInfo build(Object data) {
        return new ResponseInfo(data);
    }

    private ResponseInfo(int code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    private ResponseInfo(int code, String message) {
        this(code, message, null);
    }

    private ResponseInfo(Object data) {
        this(ResponseInfo.CODE_OK, "success", data);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
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