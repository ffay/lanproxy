package org.fengfei.lanproxy.client.handlers;

import org.fengfei.lanproxy.client.ClientChannelMannager;
import org.fengfei.lanproxy.client.listener.ChannelStatusListener;
import org.fengfei.lanproxy.client.listener.ProxyChannelBorrowListener;
import org.fengfei.lanproxy.common.Config;
import org.fengfei.lanproxy.protocol.Constants;
import org.fengfei.lanproxy.protocol.ProxyMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.IOException;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @author fengfei
 */
public class ClientChannelHandler extends SimpleChannelInboundHandler<ProxyMessage> {

    private static Logger logger = LoggerFactory.getLogger(ClientChannelHandler.class);

    private Bootstrap realServerBootstrap;

    private Bootstrap proxyBootstrap;

    private ChannelStatusListener channelStatusListener;


    public ClientChannelHandler(Bootstrap realServerBootstrap, Bootstrap proxyBootstrap, ChannelStatusListener channelStatusListener) {
        this.realServerBootstrap = realServerBootstrap;
        this.proxyBootstrap = proxyBootstrap;
        this.channelStatusListener = channelStatusListener;
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
                final ChannelHandlerContext cctx = ctx;
                final ProxyMessage cproxyMessage = proxyMessage;
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        handleUdpConnect(cctx, cproxyMessage);
                    }
                });
                thread.start();
                break;
            default:
                break;
        }
    }

    private void handleUdpConnect(ChannelHandlerContext ctx, ProxyMessage proxyMessage) {
        try {
            String requestAddress = new String(proxyMessage.getData());
            String[] ipInfo = requestAddress.split(":");
            String ip = ipInfo[0];
            int port = Integer.parseInt(ipInfo[1]);

            //发送一个udp包测试是否打洞成功
            DatagramSocket socket = new DatagramSocket();
            while (true) {
                byte[] bytes = ("connected" + System.currentTimeMillis()).getBytes();
                socket.send(new DatagramPacket(bytes, bytes.length, new InetSocketAddress(ip, port)));
                byte[] buf = new byte[1024];
                DatagramPacket receiveP = new DatagramPacket(buf, 1024);
                socket.receive(receiveP);
                System.out.println("端口:" + socket.getPort() + "接收到:" + new String(receiveP.getData()));
                TimeUnit.SECONDS.sleep(2);
            }
        } catch (Exception e) {
            e.printStackTrace();
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
