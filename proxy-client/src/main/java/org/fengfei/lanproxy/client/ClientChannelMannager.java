package org.fengfei.lanproxy.client;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.util.AttributeKey;

/**
 * 代理客户端与后端真实服务器连接管理
 *
 * @author fengfei
 *
 */
public class ClientChannelMannager {

    private static Logger logger = LoggerFactory.getLogger(ClientChannelMannager.class);

    private static final AttributeKey<String> USER_ID = AttributeKey.newInstance("user_id");

    private static final AttributeKey<Boolean> USER_CHANNEL_WRITEABLE = AttributeKey
            .newInstance("user_channel_writeable");

    private static final AttributeKey<Boolean> CLIENT_CHANNEL_WRITEABLE = AttributeKey
            .newInstance("client_channel_writeable");

    private static Map<String, Channel> realServerChannels = new ConcurrentHashMap<String, Channel>();

    private static volatile Channel channel;

    public static void setChannel(Channel channel) {
        ClientChannelMannager.channel = channel;
    }

    public static Channel getChannel() {
        return channel;
    }

    public static void setRealServerChannelUserId(Channel realServerChannel, String userId) {
        realServerChannel.attr(USER_ID).set(userId);
    }

    public static String getRealServerChannelUserId(Channel realServerChannel) {
        return realServerChannel.attr(USER_ID).get();
    }

    public static Channel getRealServerChannel(String userId) {
        return realServerChannels.get(userId);
    }

    public static void addRealServerChannel(String userId, Channel realServerChannel) {
        realServerChannels.put(userId, realServerChannel);
    }

    public static Channel removeRealServerChannel(String userId) {
        return realServerChannels.remove(userId);
    }

    public static boolean isRealServerReadable(Channel realServerChannel) {
        return realServerChannel.attr(CLIENT_CHANNEL_WRITEABLE).get()
                && realServerChannel.attr(USER_CHANNEL_WRITEABLE).get();
    }

    public static void setRealServerChannelReadability(Channel realServerChannel, Boolean client, Boolean user) {
        logger.debug("update real server channel readability, {} {} {}", realServerChannel, client, user);
        if (realServerChannel == null) {
            return;
        }

        if (client != null) {
            realServerChannel.attr(CLIENT_CHANNEL_WRITEABLE).set(client);
        }

        if (user != null) {
            realServerChannel.attr(USER_CHANNEL_WRITEABLE).set(user);
        }

        if (realServerChannel.attr(CLIENT_CHANNEL_WRITEABLE).get()
                && realServerChannel.attr(USER_CHANNEL_WRITEABLE).get()) {
            realServerChannel.config().setOption(ChannelOption.AUTO_READ, true);
        } else {
            realServerChannel.config().setOption(ChannelOption.AUTO_READ, false);
        }
    }

    public static void notifyChannelWritabilityChanged(Channel channel) {
        logger.debug("channel writability changed, {}", channel.isWritable());
        Iterator<Entry<String, Channel>> entryIte = realServerChannels.entrySet().iterator();
        while (entryIte.hasNext()) {
            setRealServerChannelReadability(entryIte.next().getValue(), channel.isWritable(), null);
        }
    }

    public static void clearRealServerChannels() {
        logger.warn("channel closed, clear real server channels");

        Iterator<Entry<String, Channel>> ite = realServerChannels.entrySet().iterator();
        while (ite.hasNext()) {
            Channel realServerChannel = ite.next().getValue();
            if (realServerChannel.isActive()) {
                realServerChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
            }
        }

        realServerChannels.clear();
    }
}
