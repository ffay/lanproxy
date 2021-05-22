package org.fengfei.lanproxy.client.handlers;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import org.fengfei.lanproxy.client.ClientChannelMannager;
import org.fengfei.lanproxy.client.listener.ChannelStatusListener;
import org.fengfei.lanproxy.client.listener.ProxyChannelBorrowListener;
import org.fengfei.lanproxy.common.Config;
import org.fengfei.lanproxy.protocol.Constants;
import org.fengfei.lanproxy.protocol.ProxyMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author fengfei
 */
public class ClientChannelHandler extends SimpleChannelInboundHandler<ProxyMessage> {

    private static Logger logger = LoggerFactory.getLogger(ClientChannelHandler.class);

    private Bootstrap realServerBootstrap;


    private Bootstrap proxyBootstrap;

    private ChannelStatusListener channelStatusListener;


    private UdpClientHandler udpClientHandler;

    public ClientChannelHandler(Bootstrap realServerBootstrap, Bootstrap proxyBootstrap, Bootstrap udpRealServerBootStrap, ChannelStatusListener channelStatusListener) {
        this.realServerBootstrap = realServerBootstrap;
        this.proxyBootstrap = proxyBootstrap;
        this.channelStatusListener = channelStatusListener;
        this.udpClientHandler = new UdpClientHandler(udpRealServerBootStrap);
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, ProxyMessage proxyMessage) throws Exception {
        logger.debug("recieved proxy message, type is {}", proxyMessage.getType());
        switch (proxyMessage.getType()) {
            case ProxyMessage.TYPE_CONNECT:
                handleConnectMessage(ctx, proxyMessage);
                break;
            case ProxyMessage.TYPE_DISCONNECT:
                handleDisconnectMessage(ctx, proxyMessage);
                break;
            case ProxyMessage.P_TYPE_TRANSFER:
                handleTransferMessage(ctx, proxyMessage);
                break;
            case ProxyMessage.TYPE_UDP_CONNECT:
                udpClientHandler.handleUdpProxy(ctx, proxyMessage);
                break;
            default:
                break;
        }
    }

    private void handleTransferMessage(ChannelHandlerContext proxyCtx, ProxyMessage proxyMessage) {
        Channel realServerChannel = proxyCtx.channel().attr(Constants.NEXT_CHANNEL).get();
        if (realServerChannel != null) {
            ByteBuf buf = proxyCtx.alloc().buffer(proxyMessage.getData().length);
            buf.writeBytes(proxyMessage.getData());
            logger.debug("write data to real server, {}", realServerChannel);
            realServerChannel.writeAndFlush(buf);
        }
    }

    private void handleDisconnectMessage(ChannelHandlerContext proxyCtx, ProxyMessage proxyMessage) {
        Channel realServerChannel = proxyCtx.channel().attr(Constants.NEXT_CHANNEL).get();
        logger.debug("handleDisconnectMessage, {}", realServerChannel);
        if (realServerChannel != null) {
            proxyCtx.channel().attr(Constants.NEXT_CHANNEL).remove();
            ClientChannelMannager.returnProxyChanel(proxyCtx.channel());
            realServerChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }

    private void handleConnectMessage(final ChannelHandlerContext clientCtx, ProxyMessage proxyMessage) {
        final Channel clientChannel = clientCtx.channel();
        final String userId = proxyMessage.getUri();
        String[] serverInfo = new String(proxyMessage.getData()).split(":");
        String ip = serverInfo[0];
        int port = Integer.parseInt(serverInfo[1]);
        realServerBootstrap.connect(ip, port).addListener(new ChannelFutureListener() {

            @Override
            public void operationComplete(ChannelFuture future) {

                // 连接后端服务器成功
                if (future.isSuccess()) {
                    final Channel realServerChannel = future.channel();
                    logger.debug("connect realserver success, {}", realServerChannel);

                    realServerChannel.config().setOption(ChannelOption.AUTO_READ, false);

                    ProxyChannelBorrowListener proxyChannelBorrowListener = new ProxyChannelBorrowListener() {

                        @Override
                        public void success(Channel proxyChannel) {
                            // 连接绑定
                            proxyChannel.attr(Constants.NEXT_CHANNEL).set(realServerChannel);
                            realServerChannel.attr(Constants.NEXT_CHANNEL).set(proxyChannel);
                            // 远程绑定
                            ProxyMessage proxyMessage = new ProxyMessage();
                            proxyMessage.setType(ProxyMessage.TYPE_CONNECT);
                            proxyMessage.setUri(userId + "@" + Config.getInstance().getStringValue("client.key"));
                            proxyChannel.writeAndFlush(proxyMessage);

                            realServerChannel.config().setOption(ChannelOption.AUTO_READ, true);
                            ClientChannelMannager.addRealServerChannel(userId, realServerChannel);
                            ClientChannelMannager.setRealServerChannelUserId(realServerChannel, userId);
                        }

                        @Override
                        public void error(Throwable cause) {
                            ProxyMessage proxyMessage = new ProxyMessage();
                            proxyMessage.setType(ProxyMessage.TYPE_DISCONNECT);
                            proxyMessage.setUri(userId);
                            clientChannel.writeAndFlush(proxyMessage);
                        }
                    };
                    // 获取连接
                    ClientChannelMannager.borrowProxyChanel(proxyBootstrap, proxyChannelBorrowListener);


                } else {
                    ProxyMessage proxyMessage = new ProxyMessage();
                    proxyMessage.setType(ProxyMessage.TYPE_DISCONNECT);
                    proxyMessage.setUri(userId);
                    clientChannel.writeAndFlush(proxyMessage);
                }
            }
        });
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        Channel realServerChannel = ctx.channel().attr(Constants.NEXT_CHANNEL).get();
        if (realServerChannel != null) {
            realServerChannel.config().setOption(ChannelOption.AUTO_READ, ctx.channel().isWritable());
        }

        super.channelWritabilityChanged(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {

        logger.info("channel Inactive->{}", ctx.channel());

        // 控制连接
        if (ClientChannelMannager.getCmdChannel() == ctx.channel()) {
            ClientChannelMannager.setCmdChannel(null);
            ClientChannelMannager.clearRealServerChannels();
            channelStatusListener.channelInactive(ctx);
        } else {
            // 数据传输连接
            Channel realServerChannel = ctx.channel().attr(Constants.NEXT_CHANNEL).get();
            if (realServerChannel != null && realServerChannel.isActive()) {
                realServerChannel.close();
            }
        }

        ClientChannelMannager.removeProxyChanel(ctx.channel());
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("exception caught", cause);
        super.exceptionCaught(ctx, cause);
    }

}
