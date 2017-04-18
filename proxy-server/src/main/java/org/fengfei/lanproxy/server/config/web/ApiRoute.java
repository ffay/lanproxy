package org.fengfei.lanproxy.server.config.web;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.fengfei.lanproxy.server.config.web.exception.ContextException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.codec.http.FullHttpRequest;

/**
 * 接口路由管理
 *
 * @author fengfei
 *
 */
public class ApiRoute {

    private static Logger logger = LoggerFactory.getLogger(ApiRoute.class);

    /** 接口路由 */
    private static Map<String, RequestHandler> routes = new ConcurrentHashMap<String, RequestHandler>();

    /** 拦截器，初始化后不会在变化 */
    private static List<RequestMiddleware> middlewares = new ArrayList<RequestMiddleware>();

    /**
     * 增加接口请求路由
     *
     * @param uri
     * @param requestHandler
     */
    public static void addRoute(String uri, RequestHandler requestHandler) {
        if (routes.containsKey(uri)) {
            throw new IllegalArgumentException("Duplicate uri:" + uri);
        }

        logger.info("add route {}", uri);
        routes.put(uri, requestHandler);
    }

    /**
     * 增加拦截器
     *
     * @param requestMiddleware
     */
    public static void addMiddleware(RequestMiddleware requestMiddleware) {
        if (middlewares.contains(requestMiddleware)) {
            throw new IllegalArgumentException("Duplicate RequestMiddleware:" + requestMiddleware);
        }

        logger.info("add requestMiddleware {}", requestMiddleware);
        middlewares.add(requestMiddleware);
    }

    /**
     * 请求执行
     *
     * @param request
     * @return
     */
    public static ResponseInfo run(FullHttpRequest request) {
        try {

            // 拦截器中如果不能通过以异常的方式进行反馈
            for (RequestMiddleware middleware : middlewares) {
                middleware.preRequest(request);
            }

            URI uri = new URI(request.getUri());
            RequestHandler handler = routes.get(uri.getPath());
            ResponseInfo responseInfo = null;
            if (handler != null) {
                responseInfo = handler.request(request);
            } else {
                responseInfo = ResponseInfo.build(ResponseInfo.CODE_API_NOT_FOUND, "api not found");
            }

            return responseInfo;
        } catch (Exception ex) {
            if (ex instanceof ContextException) {
                return ResponseInfo.build(((ContextException) ex).getCode(), ex.getMessage());
            }

            logger.error("request error", ex);
        }

        return null;
    }
}
