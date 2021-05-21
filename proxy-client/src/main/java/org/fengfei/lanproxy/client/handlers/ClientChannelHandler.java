package org.fengfei.lanproxy.client.handlers;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
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

import java.io.IOException;
import java.net.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author fengfei
 */
public class ClientChannelHandler extends SimpleChannelInboundHandler<ProxyMessage> {

    private static Logger logger = LoggerFactory.getLogger(ClientChannelHandler.class);

    private Bootstrap realServerBootstrap;

    private Bootstrap udpRealServerBootStrap;

    private Bootstrap proxyBootstrap;

    private ChannelStatusListener channelStatusListener;

    private Executor udpThreadPool = Executors.newCachedThreadPool();


    public ClientChannelHandler(Bootstrap realServerBootstrap, Bootstrap proxyBootstrap, Bootstrap udpRealServerBootStrap, ChannelStatusListener channelStatusListener) {
        this.realServerBootstrap = realServerBootstrap;
        this.proxyBootstrap = proxyBootstrap;
        this.channelStatusListener = channelStatusListener;
        this.udpRealServerBootStrap = udpRealServerBootStrap;
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
                udpThreadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        handleUdpConnect(cctx, cproxyMessage);
                    }
                });
                break;
            default:
                break;
        }
    }


    static byte[] udpPolePunchingInfo = new byte[128];

    static {
        for (int i = 0; i < 128; i++) {
            if ((i & 1) == 0) {
                udpPolePunchingInfo[i] = Byte.MAX_VALUE;
            } else {
                udpPolePunchingInfo[i] = Byte.MIN_VALUE;
            }
        }
    }


    private void handleUdpConnect(ChannelHandlerContext ctx, ProxyMessage proxyMessage) {
        try {
            String requestInfo = new String(proxyMessage.getData());


            final String[] ipInfos = requestInfo.split("-");
            final String[] userClientIpInfo = ipInfos[0].split(":");
            final String[] targetServerIpInfo = ipInfos[1].split(":");

            final String userClientIp = userClientIpInfo[0];
            final Integer userClientPort = Integer.parseInt(userClientIpInfo[1]);

            String targetServerIp = targetServerIpInfo[0];
            Integer targetServerPort = Integer.parseInt(targetServerIpInfo[1]);


            //阻塞发送打洞包测试是否连接成功
            final DatagramSocket socket = new DatagramSocket();

            socket.send(new DatagramPacket(udpPolePunchingInfo, udpPolePunchingInfo.length, new InetSocketAddress(userClientIp, userClientPort)));

            byte[] buf = new byte[128];
            final DatagramPacket receiveP = new DatagramPacket(buf, buf.length);

            socket.receive(receiveP);
            boolean connectSuccess = checkUdpPole(udpPolePunchingInfo, receiveP.getData());
            if (!connectSuccess) return;


            //调用开启真实服务端口
            final Channel realServerChannel = doConnectRealServer(targetServerIp, targetServerPort, ipInfos[0]);


            final AtomicBoolean userClientAlive = new AtomicBoolean(true);


            //开启一个线程用以处理心跳连接
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (userClientAlive.get()) {
                        try {
                            byte[] heartbeatInfo = new byte[]{Byte.MAX_VALUE, Byte.MIN_VALUE};
                            socket.send(new DatagramPacket(heartbeatInfo, heartbeatInfo.length, new InetSocketAddress(userClientIp, userClientPort)));
                            TimeUnit.SECONDS.sleep(10);
                        } catch (InterruptedException | IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }, "userClient:" + ipInfos[0]).start();


            final DatagramPacket dataReceivedP = new DatagramPacket(new byte[2048], 2048);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        //设置超时时间 10s
                        socket.setSoTimeout(1000 * 20);
                    } catch (SocketException e) {
                        e.printStackTrace();
                    }
                    while (userClientAlive.get()) {
                        try {
                            socket.receive(dataReceivedP);
                            //判断 收到的数据是否为心跳数据
                            if (dataReceivedP.getData()[0] == Byte.MAX_VALUE && dataReceivedP.getData()[1] == Byte.MIN_VALUE) {
                                logger.info("heartBeatInfo from :" + ipInfos[0]);
                                continue;
                            }
                            logger.info("user data from :" + ipInfos[0] + " to :" + ipInfos[1]);

                            ByteBuf buf = ByteBufAllocator.DEFAULT.buffer(2048);
                            buf.writeBytes(dataReceivedP.getData());
                            realServerChannel.writeAndFlush(buf);
                        } catch (SocketTimeoutException timeoutException) {
                            logger.info("user client disConnected: " + receiveP.getAddress());
                            realServerChannel.close();
                            //关闭当前线程
                            userClientAlive.set(false);
                            break;
                        } catch (IOException e) {

                            e.printStackTrace();
                        }
                    }
                }
            }).start();


        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private Channel doConnectRealServer(final String targetServerIp, final Integer targetServerPort, final String userClientAddress) {
        ChannelFuture channelFuture = udpRealServerBootStrap.connect(targetServerIp, targetServerPort).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                if (future.isSuccess()) {
                    Channel channel = future.channel();
                    channel.attr(Constants.UDP_USER_CLIENT_IP).set(userClientAddress);
                }
            }
        });
        return channelFuture.channel();
    }

    private boolean checkUdpPole(byte[] sendInfo, byte[] receiveInfo) {
        for (int i = 0; i < sendInfo.length; i++) {
            if (sendInfo[i] != receiveInfo[i]) return false;
        }
        return true;
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
