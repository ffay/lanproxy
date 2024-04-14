package cn.dxbtech.portbridge.server.config.web;

import io.netty.handler.codec.http.FullHttpRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * 接口请求处理
 *
 * @author fengfei
 */
public abstract class RequestHandler {

    /**
     * 请求处理
     *
     * @param request
     * @return
     */
    public ResponseInfo request(FullHttpRequest request) {
        return request(request, new HashMap<>());
    }

    public ResponseInfo request(FullHttpRequest request, Map<String, String> params) {
        throw new IllegalArgumentException("not supported");
    }

    public ResponseInfo request(FullHttpRequest request, String body) {
        throw new IllegalArgumentException("not supported");
    }


}