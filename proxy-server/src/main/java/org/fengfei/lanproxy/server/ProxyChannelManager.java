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

import org.fengfei.lanproxy.server.config.ProxyConfig;
import org.fengfei.lanproxy.server.config.ProxyConfig.ConfigChangedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.util.AttributeKey;

/**
 * 代理服务连接管理（代理客户端连接+用户请求连接）
 *
 * @author fengfei
 *
 */
public class ProxyChannelManager {

    private static Logger logger = LoggerFactory.getLogger(ProxyChannelManager.class);

    private static final AttributeKey<Map<String, Channel>> USER_CHANNELS = AttributeKey.newInstance("user_channels");

    private static final AttributeKey<String> USER_ID = AttributeKey.newInstance("user_id");

    private static final AttributeKey<String> REQUEST_LAN_INFO = AttributeKey.newInstance("request_lan_info");

    private static final AttributeKey<List<Integer>> CHANNEL_PORT = AttributeKey.newInstance("channel_port");

    private static final AttributeKey<String> CHANNEL_CLIENT_KEY = AttributeKey.newInstance("channel_client_key");

    private static final AttributeKey<Boolean> PROXY_CHANNEL_WRITEABLE = AttributeKey
            .newInstance("proxy_channel_writeable");

    private static final AttributeKey<Boolean> REAL_BACKEND_SERVER_CHANNEL_WRITEABLE = AttributeKey
            .newInstance("real_backend_server_channel_writeable");

    private static Map<Integer, Channel> portChannelMapping = new ConcurrentHashMap<Integer, Channel>();

    private static Map<String, Channel> proxyChannels = new ConcurrentHashMap<String, Channel>();

    static {
        ProxyConfig.getInstance().addConfigChangedListener(new ConfigChangedListener() {

            /**
             * 代理配置发生变化时回调
             */
            @Override
            public synchronized void onChanged() {
                Iterator<Entry<String, Channel>> ite = proxyChannels.entrySet().iterator();
                while (ite.hasNext()) {
                    Channel proxyChannel = ite.next().getValue();
                    String clientKey = proxyChannel.attr(CHANNEL_CLIENT_KEY).get();

                    // 去除已经去掉的clientKey配置
                    Set<String> clientKeySet = ProxyConfig.getInstance().getClientKeySet();
                    if (!clientKeySet.contains(clientKey)) {
                        removeProxyChannel(proxyChannel);
                        continue;
                    }

                    if (proxyChannel.isActive()) {
                        List<Integer> inetPorts = new ArrayList<Integer>(
                                ProxyConfig.getInstance().getClientInetPorts(clientKey));
                        Set<Integer> inetPortSet = new HashSet<Integer>(inetPorts);
                        List<Integer> channelInetPorts = new ArrayList<Integer>(proxyChannel.attr(CHANNEL_PORT).get());

                        synchronized (portChannelMapping) {

                            // 移除旧的连接映射关系
                            for (int chanelInetPort : channelInetPorts) {
                                Channel channel = portChannelMapping.get(chanelInetPort);
                                if (channel == null) {
                                    continue;
                                }

                                // 判断是否是同一个连接对象，有可能之前已经更换成其他client的连接了
                                if (proxyChannel == channel) {
                                    if (!inetPortSet.contains(chanelInetPort)) {

                                        // 移除新配置中不包含的端口
                                        portChannelMapping.remove(chanelInetPort);
                                        proxyChannel.attr(CHANNEL_PORT).get().remove(new Integer(chanelInetPort));
                                    } else {

                                        // 端口已经在改proxyChannel中使用了
                                        inetPorts.remove(new Integer(chanelInetPort));
                                    }
                                }
                            }

                            // 将新配置中增加的外网端口写入到映射配置中
                            for (int inetPort : inetPorts) {
                                portChannelMapping.put(inetPort, proxyChannel);
                                proxyChannel.attr(CHANNEL_PORT).get().add(inetPort);
                            }

                            checkAndClearUserChannels(proxyChannel);
                        }
                    }
                }

                ite = proxyChannels.entrySet().iterator();
                while (ite.hasNext()) {
                    Entry<String, Channel> entry = ite.next();
                    Channel proxyChannel = entry.getValue();
                    logger.info("proxyChannel config, {}, {}, {} ,{}", entry.getKey(), proxyChannel,
                            getUserChannels(proxyChannel).size(), proxyChannel.attr(CHANNEL_PORT).get());
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
     * 增加代理服务器端口与代理客户端连接的映射关系
     *
     * @param ports
     * @param channel
     */
    public static void addProxyChannel(List<Integer> ports, String clientKey, Channel channel) {

        if (ports == null) {
            throw new IllegalArgumentException("port can not be null");
        }

        // 客户端（proxy-client）相对较少，这里同步的比较重
        // 保证服务器对外端口与客户端到服务器的连接关系在临界情况时调用removeChannel(Channel channel)时不出问题
        synchronized (portChannelMapping) {
            for (int port : ports) {
                portChannelMapping.put(port, channel);
            }
        }

        channel.attr(CHANNEL_PORT).set(ports);
        channel.attr(CHANNEL_CLIENT_KEY).set(clientKey);
        channel.attr(USER_CHANNELS).set(new ConcurrentHashMap<String, Channel>());
        proxyChannels.put(clientKey, channel);
    }

    /**
     * 代理客户端连接断开后清除关系
     *
     * @param channel
     */
    public static void removeProxyChannel(Channel channel) {
        logger.warn("channel closed, clear user channels, {}", channel);
        if (channel.attr(CHANNEL_PORT).get() == null) {
            return;
        }

        String clientKey = channel.attr(CHANNEL_CLIENT_KEY).get();
        Channel channel0 = proxyChannels.remove(clientKey);
        if (channel != channel0) {
            proxyChannels.put(clientKey, channel);
        }

        List<Integer> ports = channel.attr(CHANNEL_PORT).get();
        for (int port : ports) {
            Channel proxyChannel = portChannelMapping.remove(port);
            if (proxyChannel == null) {
                continue;
            }

            // 在执行断连之前新的连接已经连上来了
            if (proxyChannel != channel) {
                portChannelMapping.put(port, proxyChannel);
            }
        }

        if (channel.isActive()) {
            logger.info("disconnect proxy channel {}", channel);
            channel.close();
        }

        Map<String, Channel> userChannels = getUserChannels(channel);
        Iterator<String> ite = userChannels.keySet().iterator();
        while (ite.hasNext()) {
            Channel userChannel = userChannels.get(ite.next());
            if (userChannel.isActive()) {
                userChannel.close();
                logger.info("disconnect user channel {}", userChannel);
            }
        }
    }

    public static Channel getChannel(Integer port) {
        return portChannelMapping.get(port);
    }

    public static Channel getProxyChannel(String clientKey) {
        return proxyChannels.get(clientKey);
    }

    /**
     * 增加用户连接与代理客户端连接关系
     *
     * @param proxyChannel
     * @param userId
     * @param userChannel
     */
    public static void addUserChannel(Channel proxyChannel, String userId, Channel userChannel) {
        InetSocketAddress sa = (InetSocketAddress) userChannel.localAddress();
        String lanInfo = ProxyConfig.getInstance().getLanInfo(sa.getPort());
        userChannel.attr(USER_ID).set(userId);
        userChannel.attr(REQUEST_LAN_INFO).set(lanInfo);
        proxyChannel.attr(USER_CHANNELS).get().put(userId, userChannel);
    }

    /**
     * 删除用户连接与代理客户端连接关系
     *
     * @param proxyChannel
     * @param userId
     * @return
     */
    public static Channel removeUserChannel(Channel proxyChannel, String userId) {
        synchronized (proxyChannel) {
            return proxyChannel.attr(USER_CHANNELS).get().remove(userId);
        }
    }

    /**
     * 根据代理客户端连接与用户编号获取用户连接
     *
     * @param proxyChannel
     * @param userId
     * @return
     */
    public static Channel getUserChannel(Channel proxyChannel, String userId) {
        return proxyChannel.attr(USER_CHANNELS).get().get(userId);
    }

    /**
     * 获取用户编号
     *
     * @param userChannel
     * @return
     */
    public static String getUserChannelUserId(Channel userChannel) {
        return userChannel.attr(USER_ID).get();
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
     * 获取代理客户端连接绑定的所有用户连接
     *
     * @param proxyChannel
     * @return
     */
    public static Map<String, Channel> getUserChannels(Channel proxyChannel) {
        return proxyChannel.attr(USER_CHANNELS).get();
    }

    /**
     * 更新用户连接是否可写状态
     *
     * @param userChannel
     * @param client
     * @param proxy
     */
    public static void setUserChannelReadability(Channel userChannel, Boolean realBackendServerChannelWriteability,
            Boolean proxyChannelWriteability) {
        logger.debug("update user channel readability, {} {} {}", userChannel, realBackendServerChannelWriteability,
                proxyChannelWriteability);
        synchronized (userChannel) {
            if (realBackendServerChannelWriteability != null) {
                userChannel.attr(REAL_BACKEND_SERVER_CHANNEL_WRITEABLE).set(realBackendServerChannelWriteability);
            }

            if (proxyChannelWriteability != null) {
                userChannel.attr(PROXY_CHANNEL_WRITEABLE).set(proxyChannelWriteability);
            }

            if (userChannel.attr(REAL_BACKEND_SERVER_CHANNEL_WRITEABLE).get()
                    && userChannel.attr(PROXY_CHANNEL_WRITEABLE).get()) {

                // 代理客户端与后端服务器连接状态均为可写时，用户连接状态为可读
                userChannel.config().setOption(ChannelOption.AUTO_READ, true);
            } else {
                userChannel.config().setOption(ChannelOption.AUTO_READ, false);
            }
        }
    }

    /**
     * 代理客户端连接写状态发生变化，更新关联的所有用户连接读状态
     *
     * @param proxyChannel
     */
    public static void notifyProxyChannelWritabilityChanged(Channel proxyChannel) {

        Map<String, Channel> userChannels = getUserChannels(proxyChannel);
        Iterator<String> ite = userChannels.keySet().iterator();

        // ConcurrentHashMap支持遍历过程中增删元素，所以这里不需要与增删方法同步
        while (ite.hasNext()) {
            Channel userChannel = userChannels.get(ite.next());
            setUserChannelReadability(userChannel, null, proxyChannel.isWritable());
        }
    }

}
