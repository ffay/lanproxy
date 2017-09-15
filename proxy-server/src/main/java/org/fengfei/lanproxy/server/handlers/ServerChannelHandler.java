package org.fengfei.lanproxy.server.handlers;

import java.util.List;

import org.fengfei.lanproxy.protocol.ProxyMessage;
import org.fengfei.lanproxy.server.ProxyChannelManager;
import org.fengfei.lanproxy.server.config.ProxyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 *
 * @author fengfei
 *
 */
public class ServerChannelHandler extends SimpleChannelInboundHandler<ProxyMessage> {

    private static Logger logger = LoggerFactory.getLogger(ServerChannelHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ProxyMessage proxyMessage) throws Exception {
        logger.debug("ProxyMessage received {}", proxyMessage.getType());
        switch (proxyMessage.getType()) {
        case ProxyMessage.TYPE_HEARTBEAT:
            handleHeartbeatMessage(ctx, proxyMessage);
            break;
        case ProxyMessage.TYPE_AUTH:
            handleAuthMessage(ctx, proxyMessage);
            break;
        case ProxyMessage.TYPE_CONNECT:
            handleConnectMessage(ctx, proxyMessage);
            break;
        case ProxyMessage.TYPE_DISCONNECT:
            handleDisconnectMessage(ctx, proxyMessage);
            break;
        case ProxyMessage.TYPE_TRANSFER:
            handleTransferMessage(ctx, proxyMessage);
            break;
        case ProxyMessage.TYPE_WRITE_CONTROL:
            handleWriteControlMessage(ctx, proxyMessage);
            break;
        default:
            break;
        }
    }

    private void handleWriteControlMessage(ChannelHandlerContext ctx, ProxyMessage proxyMessage) {
        String userId = proxyMessage.getUri();
        Channel userChannel = ProxyChannelManager.getUserChannel(ctx.channel(), userId);
        if (userChannel != null) {

            // 同步代理客户端与后端服务器的连接可写状态
            boolean writeable = proxyMessage.getData()[0] == 0x01 ? true : false;
            ProxyChannelManager.setUserChannelReadability(userChannel, writeable, null);
        }
    }

    private void handleTransferMessage(ChannelHandlerContext ctx, ProxyMessage proxyMessage) {
        String userId = proxyMessage.getUri();
        Channel userChannel = ProxyChannelManager.getUserChannel(ctx.channel(), userId);
        if (userChannel != null) {
            ByteBuf buf = ctx.alloc().buffer(proxyMessage.getData().length);
            buf.writeBytes(proxyMessage.getData());
            userChannel.writeAndFlush(buf);
        }
    }

    private void handleDisconnectMessage(ChannelHandlerContext ctx, ProxyMessage proxyMessage) {
        String userId = proxyMessage.getUri();
        Channel userChannel = ProxyChannelManager.removeUserChannel(ctx.channel(), userId);
        if (userChannel != null) {
            // 数据发送完成后再关闭连接，解决http1.0数据传输问题
            userChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }

    private void handleConnectMessage(ChannelHandlerContext ctx, ProxyMessage proxyMessage) {
        String userId = proxyMessage.getUri();
        Channel userChannel = ProxyChannelManager.getUserChannel(ctx.channel(), userId);
        if (userChannel != null) {

            // 代理客户端与后端服务器连接成功，修改用户连接为可读状态
            ProxyChannelManager.setUserChannelReadability(userChannel, true, ctx.channel().isWritable());
        }
    }

    private void handleHeartbeatMessage(ChannelHandlerContext ctx, ProxyMessage proxyMessage) {
        ProxyMessage heartbeatMessage = new ProxyMessage();
        heartbeatMessage.setSerialNumber(heartbeatMessage.getSerialNumber());
        heartbeatMessage.setType(ProxyMessage.TYPE_HEARTBEAT);
        logger.debug("response heartbeat message {}", heartbeatMessage);
        ctx.channel().writeAndFlush(heartbeatMessage);
    }

    private void handleAuthMessage(ChannelHandlerContext ctx, ProxyMessage proxyMessage) {
        String clientKey = proxyMessage.getUri();
        List<Integer> ports = ProxyConfig.getInstance().getClientInetPorts(clientKey);
        if (ports == null) {
            logger.info("error clientKey {}, {}", clientKey, ctx.channel());
            ctx.channel().close();
            return;
        }

        Channel channel = ProxyChannelManager.getProxyChannel(clientKey);
        if (channel != null) {
            logger.warn("exist channel for key {}, {}", clientKey, channel);
            ctx.channel().close();
            return;
        }

        logger.info("set port => channel, {}, {}, {}", clientKey, ports, ctx.channel());
        ProxyChannelManager.addProxyChannel(ports, clientKey, ctx.channel());
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        ProxyChannelManager.notifyProxyChannelWritabilityChanged(ctx.channel());
        super.channelWritabilityChanged(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ProxyChannelManager.removeProxyChannel(ctx.channel());
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("exception caught", cause);
        super.exceptionCaught(ctx, cause);
    }
}