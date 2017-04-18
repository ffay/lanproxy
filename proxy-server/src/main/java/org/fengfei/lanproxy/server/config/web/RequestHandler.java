package org.fengfei.lanproxy.server.config.web;

import io.netty.handler.codec.http.FullHttpRequest;

/**
 * 接口请求处理
 *
 * @author fengfei
 *
 */
public interface RequestHandler {

    /**
     * 请求处理
     *
     * @param request
     * @return
     */
    ResponseInfo request(FullHttpRequest request);
}