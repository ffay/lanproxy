package org.fengfei.lanproxy.server.handlers;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicLong;

import org.fengfei.lanproxy.protocol.ProxyMessage;
import org.fengfei.lanproxy.server.ProxyChannelManager;
import org.fengfei.lanproxy.server.config.ProxyConfig;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * 处理服务端 channel.
 */
public class UserChannelHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private static AtomicLong userIdProducer = new AtomicLong(0);

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {

        // 当出现异常就关闭连接
        ctx.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf buf) throws Exception {

        // 通知代理客户端
        Channel userChannel = ctx.channel();
        InetSocketAddress sa = (InetSocketAddress) userChannel.localAddress();
        Channel proxyChannel = ProxyChannelManager.getChannel(sa.getPort());
        if (proxyChannel == null) {

            // 该端口还没有代理客户端
            ctx.channel().close();
        } else {
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            String userId = ProxyChannelManager.getUserChannelUserId(userChannel);
            ProxyMessage proxyMessage = new ProxyMessage();
            proxyMessage.setType(ProxyMessage.TYPE_TRANSFER);
            proxyMessage.setUri(userId);
            proxyMessage.setData(bytes);
            proxyChannel.writeAndFlush(proxyMessage);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel userChannel = ctx.channel();
        InetSocketAddress sa = (InetSocketAddress) userChannel.localAddress();
        Channel proxyChannel = ProxyChannelManager.getChannel(sa.getPort());

        if (proxyChannel == null) {

            // 该端口还没有代理客户端
            ctx.channel().close();
        } else {
            String userId = newUserId();
            ProxyChannelManager.setUserChannelReadability(userChannel, false, proxyChannel.isWritable());
            ProxyChannelManager.addUserChannel(proxyChannel, userId, userChannel);
            ProxyMessage proxyMessage = new ProxyMessage();
            proxyMessage.setType(ProxyMessage.TYPE_CONNECT);
            proxyMessage.setUri(userId);
            proxyMessage.setData(ProxyConfig.getInstance().getLanInfo(sa.getPort()).getBytes());
            proxyChannel.writeAndFlush(proxyMessage);
        }
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {

        // 通知代理客户端
        Channel userChannel = ctx.channel();
        InetSocketAddress sa = (InetSocketAddress) userChannel.localAddress();
        Channel proxyChannel = ProxyChannelManager.getChannel(sa.getPort());
        if (proxyChannel == null) {

            // 该端口还没有代理客户端
            ctx.channel().close();
        } else {

            // 通知代理客户端，用户连接已经断开
            String userId = ProxyChannelManager.getUserChannelUserId(userChannel);
            ProxyMessage proxyMessage = new ProxyMessage();
            proxyMessage.setType(ProxyMessage.TYPE_DISCONNECT);
            proxyMessage.setUri(userId);
            proxyChannel.writeAndFlush(proxyMessage);
            ProxyChannelManager.removeUserChannel(proxyChannel, userId);
        }
        super.channelInactive(ctx);
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {

        // 通知代理客户端
        Channel userChannel = ctx.channel();
        InetSocketAddress sa = (InetSocketAddress) userChannel.localAddress();
        Channel proxyChannel = ProxyChannelManager.getChannel(sa.getPort());
        if (proxyChannel == null) {

            // 该端口还没有代理客户端
            ctx.channel().close();
        } else {

            // 通知代理客户端，用户连接可写状态
            String userId = ProxyChannelManager.getUserChannelUserId(userChannel);
            ProxyMessage proxyMessage = new ProxyMessage();
            proxyMessage.setType(ProxyMessage.TYPE_WRITE_CONTROL);
            proxyMessage.setUri(userId);
            proxyMessage.setData(userChannel.isWritable() ? new byte[] { 0x01 } : new byte[] { 0x00 });
            proxyChannel.writeAndFlush(proxyMessage);
        }
        super.channelWritabilityChanged(ctx);
    }

    /**
     * 为用户连接产生ID
     *
     * @return
     */
    private static String newUserId() {
        return String.valueOf(userIdProducer.incrementAndGet());
    }
}