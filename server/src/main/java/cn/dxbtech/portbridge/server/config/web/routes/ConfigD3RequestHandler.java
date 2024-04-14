package cn.dxbtech.portbridge.server.config.web.routes;

import cn.dxbtech.portbridge.server.config.ProxyConfig;
import cn.dxbtech.portbridge.server.config.web.RequestHandler;
import cn.dxbtech.portbridge.server.config.web.ResponseInfo;
import io.netty.handler.codec.http.FullHttpRequest;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by dxb on 2018/5/17.
 */
public class ConfigD3RequestHandler extends RequestHandler {
    @Override
    public ResponseInfo request(FullHttpRequest request) {
        List<ProxyConfig.Client> clients = ProxyConfig.getInstance().getClients(true);

        // clients 转 D3 js tree 结构
        LinkedHashMap<String, Object> d3Map = new LinkedHashMap<>();

        // 外部端口
        // 客户端 -> 内部地址
        d3Map.put("name", "central");
        LinkedList<Object> rootParents = new LinkedList<>();
        LinkedList<Object> rootChildren = new LinkedList<>();
        for (ProxyConfig.Client client : clients) {
            LinkedHashMap<String, Object> clientMap = new LinkedHashMap<>();
            clientMap.put("name", client.getName());
            clientMap.put("status", client.getStatus());

            LinkedList<Object> lanList = new LinkedList<>();
            for (ProxyConfig.ClientProxyMapping mapping : client.getProxyMappings()) {

                HashMap<String, Object> inetInfo = new HashMap<>();
                String inetPort = mapping.getInetPort().toString();
                inetInfo.put("name", inetPort);
                inetInfo.put("isparent", true);
                inetInfo.put("status", client.getStatus());
                rootParents.add(inetInfo);


                HashMap<String, Object> lanInfo = new HashMap<>();
                String name = String.format("%s (%s)", mapping.getName(), mapping.getLan());
                lanInfo.put("name", name);
                lanInfo.put("port", inetPort);
                lanInfo.put("status", client.getStatus());
                lanList.add(lanInfo);
            }
            clientMap.put("children", lanList);
            rootChildren.add(clientMap);
        }

        d3Map.put("parents", rootParents);
        d3Map.put("children", rootChildren);
        return ResponseInfo.build(d3Map);
    }
}
