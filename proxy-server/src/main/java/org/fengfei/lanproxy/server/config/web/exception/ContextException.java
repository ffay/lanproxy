package org.fengfei.lanproxy.server.config.web.exception;

public class ContextException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private int code;

    public ContextException(int code, String message) {
        super(message);
        this.code = code;
    }

    public ContextException(int code) {
        super();
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
