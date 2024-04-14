package cn.dxbtech.portbridge.server.config.web.routes;

import cn.dxbtech.portbridge.commons.Config;
import cn.dxbtech.portbridge.commons.JsonUtil;
import cn.dxbtech.portbridge.commons.StringUtil;
import cn.dxbtech.portbridge.server.config.ProxyConfig;
import cn.dxbtech.portbridge.server.config.web.ApiRoute;
import cn.dxbtech.portbridge.server.config.web.RequestHandler;
import cn.dxbtech.portbridge.server.config.web.ResponseInfo;
import cn.dxbtech.portbridge.server.info.port.AbstractPortInfoGetter;
import cn.dxbtech.portbridge.server.metrics.MetricsCollector;
import com.google.gson.reflect.TypeToken;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 接口实现
 *
 * @author fengfei
 */
public class RouteConfig {

    private static Logger logger = LoggerFactory.getLogger(RouteConfig.class);

    /**
     * 管理员不能同时在多个地方登录
     */
    private static String token;

    public static void init() {

        ApiRoute.addMiddleware(request -> {
            logger.info("rest request: [{}]", request.getUri());
        });

        // 获取配置详细信息
        ApiRoute.addRoute("/config/detail", new RequestHandler() {
            @Override
            public ResponseInfo request(FullHttpRequest request) {
                return ResponseInfo.build(ProxyConfig.getInstance().getClients(true));
            }
        });

        ApiRoute.addRoute("/config/d3", new ConfigD3RequestHandler());

        // 更新配置
        ApiRoute.addRoute("/config/update", new RequestHandler() {
            @Override
            public ResponseInfo request(FullHttpRequest request, String config) {
                logger.info("config json: {}", config);
                List<ProxyConfig.Client> clients = JsonUtil.json2object(config, new TypeToken<List<ProxyConfig.Client>>() {
                });
                if (clients == null) {
                    return ResponseInfo.build(HttpResponseStatus.BAD_REQUEST, "Error json config");
                }

                try {
                    ProxyConfig.getInstance().update(config);
                } catch (Exception ex) {
                    logger.error("config update error", ex);
                    return ResponseInfo.build(HttpResponseStatus.BAD_REQUEST, ex.getMessage());
                }

                return ResponseInfo.build(HttpResponseStatus.OK, "success");
            }
        });

        ApiRoute.addRoute("/metrics/get", new RequestHandler() {
            @Override
            public ResponseInfo request(FullHttpRequest request) {
                return ResponseInfo.build(MetricsCollector.getAllMetrics());
            }
        });

        ApiRoute.addRoute("/metrics/list", new MetricsListRequestHandler());

        ApiRoute.addRoute("/metrics/getandreset", new RequestHandler() {
            @Override
            public ResponseInfo request(FullHttpRequest request) {
                return ResponseInfo.build(MetricsCollector.getAndResetAllMetrics());
            }
        });

        ApiRoute.addRoute("/info/port", new RequestHandler() {
            @Override
            public ResponseInfo request(FullHttpRequest request, Map<String, String> map) {
                try {
                    return ResponseInfo.build(AbstractPortInfoGetter.get(map.get("sort")));
                } catch (Exception e) {
                    return ResponseInfo.build(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Cannot get port using statics: " + e.toString());
                }
            }
        });

        ApiRoute.addRoute("/info/props", new RequestHandler() {
            @Override
            public ResponseInfo request(FullHttpRequest request) {
                Map<String, String> map = new LinkedHashMap<>();
                map.put("serverBind", Config.getInstance().getStringValue("server.bind", "0.0.0.0"));
                map.put("serverPort", Config.getInstance().getStringValue("server.port"));
                map.put("configServerBind", Config.getInstance().getStringValue("config.server.bind", "0.0.0.0"));
                map.put("configServerPort", Config.getInstance().getStringValue("config.server.port"));
                map.put("configAdminUsername", Config.getInstance().getStringValue("config.admin.username"));
                map.put("artifactVersion", StringUtil.isNotEmpty(System.getProperty("artifact.version")) ? "-" + System.getProperty("artifact.version") : "-0.5");
                return ResponseInfo.build(map);
            }
        });

    }


}
