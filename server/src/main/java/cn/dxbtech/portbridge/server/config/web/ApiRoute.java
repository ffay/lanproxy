package cn.dxbtech.portbridge.server.config.web;

import cn.dxbtech.portbridge.server.config.web.exception.ContextException;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.internal.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 接口路由管理
 *
 * @author fengfei
 */
public class ApiRoute {

    private static Logger logger = LoggerFactory.getLogger(ApiRoute.class);

    /**
     * 接口路由
     */
    private static Map<String, RequestHandler> routes = new ConcurrentHashMap<>();

    /**
     * 拦截器，初始化后不会在变化
     */
    private static List<RequestMiddleware> middlewares = new ArrayList<>();

    /**
     * 增加接口请求路由
     *
     * @param uri
     * @param requestHandler
     */
    public static void addRoute(String uri, RequestHandler requestHandler) {
        uri = rebuildUri(uri);
        if (routes.containsKey(uri)) {
            throw new IllegalArgumentException("Duplicate uri:" + uri);
        }

        logger.debug("add route {}", uri);
        routes.put(uri, requestHandler);
    }

    public static boolean isApi(String uri) {
        return routes.containsKey(rebuildUri(uri));
    }

    // 重组uri
    private static String rebuildUri(String uri) {
        int indexOfParam = uri.indexOf("?");
        if (indexOfParam > 0) {
            uri = uri.substring(0, indexOfParam);
        }
        String[] paths = StringUtil.split(uri, '/');
        StringBuilder uriBuilder = new StringBuilder();
        for (String path : paths) {
            if (StringUtil.isNullOrEmpty(path)) continue;
            uriBuilder.append('/').append(path);
        }
        return uriBuilder.toString();
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

        logger.info("add http handler {}", requestMiddleware);
        middlewares.add(requestMiddleware);
    }

    /**
     * 请求执行
     */
    public static ResponseInfo run(FullHttpRequest request, String body) {
        String uri = request.getUri();
        try {

            // 拦截器中如果不能通过以异常的方式进行反馈
            for (RequestMiddleware middleware : middlewares) {
                middleware.preRequest(request);
            }

            RequestHandler handler = routes.get(rebuildUri(uri));
            ResponseInfo responseInfo;
            if (handler != null) {
                if (uri.contains("?")) {
                    String query = uri.substring(uri.indexOf("?") + 1);
                    Map<String, String> queryMap = new LinkedHashMap<>();
                    for (String s : StringUtil.split(query, '&')) {
                        String[] split = StringUtil.split(s, '=');
                        if (split.length == 2) {
                            queryMap.put(split[0], split[1]);
                        } else {
                            queryMap.put(split[0], null);
                        }
                    }
                    responseInfo = handler.request(request, queryMap);
                } else if (body != null) {
                    responseInfo = handler.request(request, body);
                } else {
                    responseInfo = handler.request(request);
                }
            } else {
                responseInfo = ResponseInfo.build(HttpResponseStatus.NOT_FOUND, "api not found");
            }

            return responseInfo;
        } catch (Exception ex) {
            if (ex instanceof ContextException) {
                return ResponseInfo.build(HttpResponseStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
            }
            String err = request.getMethod() + " request failed: " + uri;
            logger.error(err, ex);
            throw new IllegalStateException(err, ex);
        }
    }
}
