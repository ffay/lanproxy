package org.fengfei.lanproxy.server.handlers;

import java.util.List;

import org.fengfei.lanproxy.protocol.ProxyMessage;
import org.fengfei.lanproxy.server.ProxyChannelManager;
import org.fengfei.lanproxy.server.config.ProxyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
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
            userChannel.close();
        }
    }

    private void handleConnectMessage(ChannelHandlerContext ctx, ProxyMessage proxyMessage) {
        String userId = proxyMessage.getUri();
        Channel userChannel = ProxyChannelManager.getUserChannel(ctx.channel(), userId);
        if (userChannel != null) {
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
            logger.info("error clientKey {}, close channel", clientKey);
            ctx.channel().close();
            return;
        }

        logger.info("set port => channel, {} {}", clientKey, ports);
        ProxyChannelManager.addChannel(ports, ctx.channel());
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        ProxyChannelManager.notifyChannelWritabilityChanged(ctx.channel());
        super.channelWritabilityChanged(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ProxyChannelManager.removeChannel(ctx.channel());
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("exception caught", cause);
        super.exceptionCaught(ctx, cause);
    }
}