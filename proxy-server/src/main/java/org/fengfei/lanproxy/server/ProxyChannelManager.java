package org.fengfei.lanproxy.server;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
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

    private static final AttributeKey<List<Integer>> CHANNEL_PORT = AttributeKey.newInstance("channel_port");

    private static final AttributeKey<Boolean> PROXY_CHANNEL_WRITEABLE = AttributeKey
            .newInstance("proxy_channel_writeable");

    private static final AttributeKey<Boolean> REAL_BACKEND_SERVER_CHANNEL_WRITEABLE = AttributeKey
            .newInstance("real_backend_server_channel_writeable");

    private static Map<Integer, Channel> proxyChannels = new ConcurrentHashMap<Integer, Channel>();

    static {
        ProxyConfig.getInstance().addConfigChangedListener(new ConfigChangedListener() {

            /**
             * 代理配置发生变化时回调
             */
            @Override
            public void onChanged() {
                Iterator<Integer> ite = proxyChannels.keySet().iterator();
                while (ite.hasNext()) {
                    Channel proxyChannel = proxyChannels.get(ite.next());
                    if (proxyChannel != null && proxyChannel.isActive()) {
                        removeProxyChannel(proxyChannel);
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
    public static void addProxyChannel(List<Integer> ports, Channel channel) {

        if (ports == null) {
            throw new IllegalArgumentException("port can not be null");
        }

        // 客户端（proxy-client）相对较少，这里同步的比较重
        // 保证服务器对外端口与客户端到服务器的连接关系在临界情况时调用removeChannel(Channel channel)时不出问题
        synchronized (proxyChannels) {

            //
            for (int port : ports) {
                proxyChannels.put(port, channel);
            }
        }

        channel.attr(CHANNEL_PORT).set(ports);
        channel.attr(USER_CHANNELS).set(new ConcurrentHashMap<String, Channel>());
    }

    /**
     * 代理客户端连接断开后清楚关系
     *
     * @param channel
     */
    public static void removeProxyChannel(Channel channel) {
        logger.warn("channel closed, clear user channels, {}", channel);
        if (channel.attr(CHANNEL_PORT).get() == null) {
            return;
        }

        List<Integer> ports = channel.attr(CHANNEL_PORT).get();
        for (int port : ports) {
            Channel proxyChannel = proxyChannels.remove(port);
            if (proxyChannel == null) {
                continue;
            }

            // 在执行断连之前新的连接已经连上来了
            if (proxyChannel != channel) {
                proxyChannels.put(port, proxyChannel);
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
        return proxyChannels.get(port);
    }

    /**
     * 增加用户连接与代理客户端连接关系
     *
     * @param proxyChannel
     * @param userId
     * @param userChannel
     */
    public static void addUserChannel(Channel proxyChannel, String userId, Channel userChannel) {
        userChannel.attr(USER_ID).set(userId);
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
        logger.info("update user channel readability, {} {} {}", userChannel, realBackendServerChannelWriteability,
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
