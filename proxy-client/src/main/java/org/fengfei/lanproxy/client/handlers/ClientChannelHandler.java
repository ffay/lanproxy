package org.fengfei.lanproxy.client.handlers;

import org.fengfei.lanproxy.client.ClientChannelMannager;
import org.fengfei.lanproxy.client.listener.ChannelStatusListener;
import org.fengfei.lanproxy.protocol.ProxyMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 *
 * @author fengfei
 *
 */
public class ClientChannelHandler extends SimpleChannelInboundHandler<ProxyMessage> {

    private static Logger logger = LoggerFactory.getLogger(ClientChannelHandler.class);

    private Bootstrap bootstrap;

    private ChannelStatusListener channelStatusListener;

    public ClientChannelHandler(Bootstrap bootstrap, ChannelStatusListener channelStatusListener) {
        this.bootstrap = bootstrap;
        this.channelStatusListener = channelStatusListener;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ProxyMessage proxyMessage) throws Exception {
        logger.debug("recieved proxy message, type is {}", proxyMessage.getType());
        switch (proxyMessage.getType()) {
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
        Channel realServerChannel = ClientChannelMannager.getRealServerChannel(userId);
        if (realServerChannel != null) {
            boolean writeable = proxyMessage.getData()[0] == 0x01 ? true : false;
            ClientChannelMannager.setRealServerChannelReadability(realServerChannel, null, writeable);
        }
    }

    private void handleTransferMessage(ChannelHandlerContext ctx, ProxyMessage proxyMessage) {
        String userId = proxyMessage.getUri();
        Channel realServerChannel = ClientChannelMannager.getRealServerChannel(userId);
        if (realServerChannel != null) {
            ByteBuf buf = ctx.alloc().buffer(proxyMessage.getData().length);
            buf.writeBytes(proxyMessage.getData());
            logger.debug("write data to real server, {}", realServerChannel);
            realServerChannel.writeAndFlush(buf);
        }
    }

    private void handleDisconnectMessage(ChannelHandlerContext ctx, ProxyMessage proxyMessage) {
        String userId = proxyMessage.getUri();
        Channel userChannel = ClientChannelMannager.removeRealServerChannel(userId);
        if (userChannel != null) {
            userChannel.close();
        }
    }

    private void handleConnectMessage(ChannelHandlerContext ctx, ProxyMessage proxyMessage) {
        final Channel channel = ctx.channel();
        final String userId = proxyMessage.getUri();
        String[] serverInfo = new String(proxyMessage.getData()).split(":");
        String ip = serverInfo[0];
        int port = Integer.parseInt(serverInfo[1]);
        bootstrap.connect(ip, port).addListener(new ChannelFutureListener() {

            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    Channel realServerChannel = future.channel();
                    ClientChannelMannager.setRealServerChannelReadability(realServerChannel, true,
                            channel.isWritable());
                    ClientChannelMannager.addRealServerChannel(userId, realServerChannel);
                    ClientChannelMannager.setRealServerChannelUserId(realServerChannel, userId);
                    ProxyMessage proxyMessage = new ProxyMessage();
                    proxyMessage.setType(ProxyMessage.TYPE_CONNECT);
                    proxyMessage.setUri(userId);
                    channel.writeAndFlush(proxyMessage);
                } else {
                    ProxyMessage proxyMessage = new ProxyMessage();
                    proxyMessage.setType(ProxyMessage.TYPE_DISCONNECT);
                    proxyMessage.setUri(userId);
                    channel.writeAndFlush(proxyMessage);
                }
            }
        });
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        ClientChannelMannager.notifyChannelWritabilityChanged(ctx.channel());
        super.channelWritabilityChanged(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ClientChannelMannager.setChannel(null);
        ClientChannelMannager.clearRealServerChannels();
        channelStatusListener.channelInactive(ctx);
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("exception caught", cause);
        super.exceptionCaught(ctx, cause);
    }

}