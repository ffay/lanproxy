package cn.dxbtech.portbridge.server.config.web.routes;

import cn.dxbtech.portbridge.commons.JsonUtil;
import cn.dxbtech.portbridge.commons.PersistenceUtil;
import cn.dxbtech.portbridge.server.config.ProxyConfig;
import cn.dxbtech.portbridge.server.config.web.RequestHandler;
import cn.dxbtech.portbridge.server.config.web.ResponseInfo;
import cn.dxbtech.portbridge.server.metrics.Metrics;
import com.google.gson.reflect.TypeToken;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Created by dxb on 2018/5/17.
 */
public class MetricsListRequestHandler extends RequestHandler {
    @Override
    public ResponseInfo request(FullHttpRequest request, Map<String, String> map) {
        try {
            return ResponseInfo.build(getMetricsList());
        } catch (IOException e) {
            return ResponseInfo.build(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Can't find metrics: " + e.toString());

        }
    }

    List<Map<String, Object>> getMetricsList() throws IOException {
        Map<String, List<Metrics>> map = new ConcurrentHashMap<>();
        Path dir = Paths.get(PersistenceUtil.getFilePath(false, "metrics"));

        if (Files.notExists(dir)) {
            return new ArrayList<>(1);
        }


        try (Stream<Path> base = Files.list(dir)) {
            base.forEach(portDir -> {
                String port = portDir.getFileName().toString();
                LinkedList<Metrics> list = new LinkedList<>();
                map.putIfAbsent(port, list);

                try (Stream<Path> portDirStream = Files.list(portDir)) {
                    portDirStream.forEach(mPath -> {
                        try {
                            list.add(JsonUtil.json2object(Files.readAllBytes(mPath), new TypeToken<Metrics>() {
                            }));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }


        List<ProxyConfig.Client> clients = ProxyConfig.getInstance().getClients(true);
        Map<String, ProxyConfig.Client> clientMap = new HashMap<>(clients.size());
        Map<String, ProxyConfig.ClientProxyMapping> proxyMap = new HashMap<>(clients.size());
        for (ProxyConfig.Client client : clients) {
            for (ProxyConfig.ClientProxyMapping clientProxyMapping : client.getProxyMappings()) {
                clientMap.put(clientProxyMapping.getInetPort() + "", client);
                proxyMap.put(clientProxyMapping.getInetPort() + "", clientProxyMapping);
            }
        }


        ArrayList<Map<String, Object>> list = new ArrayList<>(map.size());
        for (Map.Entry<String, List<Metrics>> entry : map.entrySet()) {

            String port = entry.getKey();
            List<Metrics> metrics = entry.getValue();
            metrics.sort((o1, o2) -> (int) (o1.getTimestamp() - o2.getTimestamp()));

            HashMap<String, Object> json = new HashMap<>();

            json.put("metrics", metrics);
            json.put("port", port);

            ProxyConfig.Client client = clientMap.get(port);
            ProxyConfig.ClientProxyMapping mapping = proxyMap.get(port);

            json.put("client", client.getName());
            json.put("proxy", mapping.getName());
            json.put("lan", mapping.getLan());

            list.add(json);
        }

        return list;

    }

}
