package org.fengfei.lanproxy.server;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.fengfei.lanproxy.protocol.Constants;
import org.fengfei.lanproxy.server.config.ProxyConfig;
import org.fengfei.lanproxy.server.config.ProxyConfig.ConfigChangedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

/**
 * 代理服务连接管理（代理客户端连接+用户请求连接）
 *
 * @author fengfei
 */
public class ProxyChannelManager {

    private static final Logger logger = LoggerFactory.getLogger(ProxyChannelManager.class);

    private static final AttributeKey<Map<String, Channel>> USER_CHANNELS = AttributeKey.newInstance("user_channels");

    private static final AttributeKey<String> REQUEST_LAN_INFO = AttributeKey.newInstance("request_lan_info");

    private static final AttributeKey<List<Integer>> CHANNEL_PORT = AttributeKey.newInstance("channel_port");

    private static final AttributeKey<String> CHANNEL_CLIENT_KEY = AttributeKey.newInstance("channel_client_key");

    private static final Map<Integer, Channel> portClientChannelMapping = new ConcurrentHashMap<>();

    private static final Map<String, Channel> clientKeyChannelMap = new ConcurrentHashMap<>();

    static {
        ProxyConfig.getInstance().addConfigChangedListener(new ConfigChangedListener() {

            /**
             * 代理配置发生变化时回调
             */
            @Override
            public synchronized void onChanged() {
                Iterator<Entry<String, Channel>> ite = clientKeyChannelMap.entrySet().iterator();
                while (ite.hasNext()) {
                    Channel proxyChannel = ite.next().getValue();
                    String clientKey = proxyChannel.attr(CHANNEL_CLIENT_KEY).get();

                    // 去除已经去掉的clientKey配置
                    Set<String> clientKeySet = ProxyConfig.getInstance().getClientKeySet();
                    if (!clientKeySet.contains(clientKey)) {
                        removeCmdChannel(proxyChannel);
                        continue;
                    }

                    if (proxyChannel.isActive()) {
                        List<Integer> inetPorts = new ArrayList<Integer>(ProxyConfig.getInstance().getClientInetPorts(clientKey));
                        Set<Integer> inetPortSet = new HashSet<Integer>(inetPorts);
                        List<Integer> channelInetPorts = new ArrayList<Integer>(proxyChannel.attr(CHANNEL_PORT).get());

                        synchronized (portClientChannelMapping) {

                            // 移除旧的连接映射关系
                            for (int channelInetPort : channelInetPorts) {
                                Channel channel = portClientChannelMapping.get(channelInetPort);
                                if (channel == null) {
                                    continue;
                                }

                                // 判断是否是同一个连接对象，有可能之前已经更换成其他client的连接了
                                if (proxyChannel == channel) {
                                    if (!inetPortSet.contains(channelInetPort)) {

                                        // 移除新配置中不包含的端口
                                        portClientChannelMapping.remove(channelInetPort);
                                        proxyChannel.attr(CHANNEL_PORT).get().remove(new Integer(channelInetPort));
                                    } else {

                                        // 端口已经在改proxyChannel中使用了
                                        inetPorts.remove(new Integer(channelInetPort));
                                    }
                                }
                            }

                            // 将新配置中增加的外网端口写入到映射配置中
                            for (int inetPort : inetPorts) {
                                portClientChannelMapping.put(inetPort, proxyChannel);
                                proxyChannel.attr(CHANNEL_PORT).get().add(inetPort);
                            }

                            checkAndClearUserChannels(proxyChannel);
                        }
                    }
                }

                ite = clientKeyChannelMap.entrySet().iterator();
                while (ite.hasNext()) {
                    Entry<String, Channel> entry = ite.next();
                    Channel proxyChannel = entry.getValue();
                    logger.info("proxyChannel config, {}, {}, {} ,{}", entry.getKey(), proxyChannel, getUserChannels(proxyChannel).size(), proxyChannel.attr(CHANNEL_PORT).get());
                }
            }

            /**
             * 检测连接配置是否与当前配置一致，不一致则关闭
             *
             * @param proxyChannel
             */
            private void checkAndClearUserChannels(Channel proxyChannel) {
                Map<String, Channel> userChannels = getUserChannels(proxyChannel);
                Iterator<Entry<String, Channel>> userChannelIte = userChannels.entrySet().iterator();
                while (userChannelIte.hasNext()) {
                    Entry<String, Channel> entry = userChannelIte.next();
                    Channel userChannel = entry.getValue();
                    String requestLanInfo = getUserChannelRequestLanInfo(userChannel);
                    InetSocketAddress sa = (InetSocketAddress) userChannel.localAddress();
                    String lanInfo = ProxyConfig.getInstance().getLanInfo(sa.getPort());

                    // 判断当前配置中对应外网端口的lan信息是否与正在运行的连接中的lan信息是否一致
                    if (lanInfo == null || !lanInfo.equals(requestLanInfo)) {
                        userChannel.close();

                        // ConcurrentHashMap不会报ConcurrentModificationException异常
                        userChannels.remove(entry.getKey());
                    }
                }
            }
        });
    }

    /**
     * 增加代理服务器端口与代理控制客户端连接的映射关系
     *
     * @param ports         Ports required by the client
     * @param clientChannel client->server channel
     */
    public static void addCmdChannel(List<Integer> ports, String clientKey, Channel clientChannel) {

        if (ports == null) {
            throw new IllegalArgumentException("port can not be null");
        }

        // 客户端（proxy-client）相对较少，这里同步的比较重
        // 保证服务器对外端口与客户端到服务器的连接关系在临界情况时调用removeChannel(Channel channel)时不出问题
        synchronized (portClientChannelMapping) {
            for (int port : ports) {
                //客户端与公网端口为1:N
                //映射某个端口对应某个具体的通道

                portClientChannelMapping.put(port, clientChannel);
            }
        }

        clientChannel.attr(CHANNEL_PORT).set(ports);
        clientChannel.attr(CHANNEL_CLIENT_KEY).set(clientKey);
        clientChannel.attr(USER_CHANNELS).set(new ConcurrentHashMap<String, Channel>());
        //映射某个
        clientKeyChannelMap.put(clientKey, clientChannel);
    }

    /**
     * 代理客户端连接断开后清除关系
     *
     * @param clientChannel
     */
    public static void removeCmdChannel(Channel clientChannel) {
        logger.warn("channel closed, clear user channels, {}", clientChannel);
        if (clientChannel.attr(CHANNEL_PORT).get() == null) {
            return;
        }

        String clientKey = clientChannel.attr(CHANNEL_CLIENT_KEY).get();
        Channel channel0 = clientKeyChannelMap.remove(clientKey);
        if (clientChannel != channel0) {
            clientKeyChannelMap.put(clientKey, clientChannel);
        }

        List<Integer> ports = clientChannel.attr(CHANNEL_PORT).get();
        for (int port : ports) {
            Channel proxyChannel = portClientChannelMapping.remove(port);
            if (proxyChannel == null) {
                continue;
            }

            // 在执行断连之前新的连接已经连上来了
            if (proxyChannel != clientChannel) {
                portClientChannelMapping.put(port, proxyChannel);
            }
        }

        if (clientChannel.isActive()) {
            logger.info("disconnect proxy channel {}", clientChannel);
            clientChannel.close();
        }

        Map<String, Channel> userChannels = getUserChannels(clientChannel);
        Iterator<String> ite = userChannels.keySet().iterator();
        while (ite.hasNext()) {
            Channel userChannel = userChannels.get(ite.next());
            if (userChannel.isActive()) {
                userChannel.close();
                logger.info("disconnect user channel {}", userChannel);
            }
        }
    }

    public static Channel getClientChannel(Integer serverPort) {
        return portClientChannelMapping.get(serverPort);
    }

    public static Channel getClientChannel(String clientKey) {
        return clientKeyChannelMap.get(clientKey);
    }

    /**
     * 增加用户连接与代理客户端连接关系
     *
     * @param userId
     * @param userChannel
     */
    public static void addUserChannelToCmdChannel(Channel cmdChannel, String userId, Channel userChannel) {
        InetSocketAddress sa = (InetSocketAddress) userChannel.localAddress();
        String lanInfo = ProxyConfig.getInstance().getLanInfo(sa.getPort());
        userChannel.attr(Constants.USER_ID).set(userId);
        userChannel.attr(REQUEST_LAN_INFO).set(lanInfo);
        cmdChannel.attr(USER_CHANNELS).get().put(userId, userChannel);
    }

    /**
     * 删除用户连接与代理客户端连接关系
     *
     * @param userId
     * @return
     */
    public static Channel removeUserChannelFromCmdChannel(Channel cmdChannel, String userId) {
        if (cmdChannel.attr(USER_CHANNELS).get() == null) {
            return null;
        }

        synchronized (cmdChannel) {
            return cmdChannel.attr(USER_CHANNELS).get().remove(userId);
        }
    }

    /**
     * 根据代理客户端连接与用户编号获取用户连接
     *
     * @param userId
     * @return
     */
    public static Channel getUserChannel(Channel cmdChannel, String userId) {
        return cmdChannel.attr(USER_CHANNELS).get().get(userId);
    }

    /**
     * 获取用户编号
     *
     * @param userChannel
     * @return
     */
    public static String getUserChannelUserId(Channel userChannel) {
        return userChannel.attr(Constants.USER_ID).get();
    }

    /**
     * 获取用户请求的内网IP端口信息
     *
     * @param userChannel
     * @return
     */
    public static String getUserChannelRequestLanInfo(Channel userChannel) {
        return userChannel.attr(REQUEST_LAN_INFO).get();
    }

    /**
     * 获取代理控制客户端连接绑定的所有用户连接
     *
     * @param cmdChannel
     * @return
     */
    public static Map<String, Channel> getUserChannels(Channel cmdChannel) {
        return cmdChannel.attr(USER_CHANNELS).get();
    }

}
