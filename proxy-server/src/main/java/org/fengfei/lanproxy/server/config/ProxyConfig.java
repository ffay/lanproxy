package org.fengfei.lanproxy.server.config;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.fengfei.lanproxy.common.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.reflect.TypeToken;

/**
 * server config
 *
 * @author fengfei
 *
 */
public class ProxyConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private static Logger logger = LoggerFactory.getLogger(ProxyConfig.class);

    /** 配置文件为config.json */
    private static final String DEFAULT_CONF = "config.json";

    /** 代理服务器与代理客户端通信端口 */
    private Integer serverPort;

    /** 代理客户端，支持多个客户端 */
    private List<Client> clients;

    /** 更新配置后保证在其他线程即时生效 */
    private volatile static ProxyConfig instance;

    /** 代理服务器为各个代理客户端（key）开启对应的端口列表（value） */
    private transient Map<String, List<Integer>> clientInetPortMapping = new HashMap<String, List<Integer>>();

    /** 代理服务器上的每个对外端口（key）对应的代理客户端背后的真实服务器信息（value） */
    private transient Map<Integer, String> inetPortLanInfoMapping = new HashMap<Integer, String>();

    /** 配置变化监听器 */
    private static List<ConfigChangedListener> configChangedListeners = new ArrayList<ConfigChangedListener>();

    private static ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    static {
        executor.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                try {
                    ProxyConfig.update();
                } catch (Exception ex) {
                    logger.error("Parse config file error", ex);
                }
            }

            // 5 秒更新一次配置
        }, 5000, 5000, TimeUnit.MILLISECONDS);
    }

    private List<Client> getClients() {
        return clients;
    }

    public Integer getServerPort() {
        return this.serverPort;
    }

    /**
     * 解析配置文件
     */
    public static void update() {

        instance = new ProxyConfig();
        String configJson = null;
        try {
            InputStream in = ProxyConfig.class.getClassLoader().getResourceAsStream(DEFAULT_CONF);
            byte[] buf = new byte[1024];
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int readIndex;
            while ((readIndex = in.read(buf)) != -1) {
                out.write(buf, 0, readIndex);
            }

            configJson = new String(out.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        ProxyConfig config = JsonUtil.json2object(configJson, new TypeToken<ProxyConfig>() {
        });
        if (config == null) {
            throw new RuntimeException("Error config");
        }

        Map<String, List<Integer>> clientInetPortMapping = config.getClientInetPortMapping();
        Map<Integer, String> inetPortLanInfoMapping = config.getInetPortLanInfoMapping();

        // 构造端口映射关系
        List<Client> clients = config.getClients();
        for (Client client : clients) {

            String clientKey = client.getClientKey();
            List<ClientProxyMapping> mappings = client.getProxyMappings();
            List<Integer> ports = new ArrayList<Integer>();
            clientInetPortMapping.put(clientKey, ports);
            for (ClientProxyMapping mapping : mappings) {
                Integer port = mapping.getInetPort();
                ports.add(port);
                if (inetPortLanInfoMapping.containsKey(port)) {
                    throw new IllegalArgumentException("duplicate inet port " + port);
                }

                inetPortLanInfoMapping.put(port, mapping.getLan());
            }
        }

        instance = config;
        notifyconfigChangedListeners();
    }

    /**
     * 配置更新通知
     */
    private static void notifyconfigChangedListeners() {
        List<ConfigChangedListener> changedListeners = new ArrayList<ConfigChangedListener>(configChangedListeners);
        for (ConfigChangedListener changedListener : changedListeners) {
            changedListener.onChanged();
        }
    }

    private Map<String, List<Integer>> getClientInetPortMapping() {
        return clientInetPortMapping;
    }

    private Map<Integer, String> getInetPortLanInfoMapping() {
        return inetPortLanInfoMapping;
    }

    /**
     * 添加配置变化监听器
     *
     * @param configChangedListener
     */
    public static void addConfigChangedListener(ConfigChangedListener configChangedListener) {
        configChangedListeners.add(configChangedListener);
    }

    /**
     * 移除配置变化监听器
     *
     * @param configChangedListener
     */
    public static void removeConfigChangedListener(ConfigChangedListener configChangedListener) {
        configChangedListeners.remove(configChangedListener);
    }

    public List<Integer> getClientInetPorts(String clientKey) {
        return clientInetPortMapping.get(clientKey);
    }

    public String getLanInfo(Integer port) {
        return inetPortLanInfoMapping.get(port);
    }

    public List<Integer> getUserPorts() {
        List<Integer> ports = new ArrayList<Integer>();
        Iterator<Integer> ite = inetPortLanInfoMapping.keySet().iterator();
        while (ite.hasNext()) {
            ports.add(ite.next());
        }

        return ports;
    }

    public static ProxyConfig getInstance() {
        return instance;
    }

    /**
     * 代理客户端
     *
     * @author fengfei
     *
     */
    public class Client implements Serializable {

        private static final long serialVersionUID = 1L;

        /** 代理客户端唯一标识key */
        private String clientKey;

        /** 代理客户端与其后面的真实服务器映射关系 */
        private List<ClientProxyMapping> proxyMappings;

        public String getClientKey() {
            return clientKey;
        }

        public void setClientKey(String clientKey) {
            this.clientKey = clientKey;
        }

        public List<ClientProxyMapping> getProxyMappings() {
            return proxyMappings;
        }

        public void setProxyMappings(List<ClientProxyMapping> proxyMappings) {
            this.proxyMappings = proxyMappings;
        }

    }

    /**
     * 代理客户端与其后面真实服务器映射关系
     *
     * @author fengfei
     *
     */
    public class ClientProxyMapping {

        /** 代理服务器端口 */
        private Integer inetPort;

        /** 需要代理的网络信息（代理客户端能够访问），格式 192.168.1.99:80 (必须带端口) */
        private String lan;

        public Integer getInetPort() {
            return inetPort;
        }

        public void setInetPort(Integer inetPort) {
            this.inetPort = inetPort;
        }

        public String getLan() {
            return lan;
        }

        public void setLan(String lan) {
            this.lan = lan;
        }

    }

    /**
     * 配置更新回调
     *
     * @author fengfei
     *
     */
    public static interface ConfigChangedListener {

        void onChanged();
    }
}
