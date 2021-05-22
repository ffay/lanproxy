package org.fengfei.lanproxy.client.handlers;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import lombok.Data;
import lombok.SneakyThrows;
import org.fengfei.lanproxy.protocol.Constants;
import org.fengfei.lanproxy.protocol.ProxyMessage;
import org.fengfei.lanproxy.protocol.UdpProxyMessageCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * udp 代理处理逻辑
 */
public class UdpClientHandler {
    private static final Logger logger = LoggerFactory.getLogger(UdpClientHandler.class);


    private final Executor udpThreadPool = Executors.newCachedThreadPool();


    private final Bootstrap udpRealServerBootStrap;


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

    public UdpClientHandler(Bootstrap udpRealServerBootStrap) {

        this.udpRealServerBootStrap = udpRealServerBootStrap;
    }

    public boolean doUdpPunching(DatagramSocket socket, UdpConnectInfo connectInfo) throws IOException {
        socket.send(new DatagramPacket(udpPolePunchingInfo, udpPolePunchingInfo.length, new InetSocketAddress(connectInfo.userClientIp, connectInfo.userClientPort)));
        byte[] buf = new byte[128];
        final DatagramPacket receiveP = new DatagramPacket(buf, buf.length);
        socket.receive(receiveP);
        return checkUdpPole(udpPolePunchingInfo, receiveP.getData());
    }


    @SneakyThrows
    public void handleUdpProxy(ProxyMessage proxyMessage) {


        final UdpConnectInfo connectInfo = new UdpConnectInfo(proxyMessage);

        final DatagramSocket socket = new DatagramSocket();

        final AtomicBoolean userClientAlive = new AtomicBoolean(true);


        try {


            //阻塞发送打洞包测试是否连接成功
            if (!doUdpPunching(socket, connectInfo)) return;


            final ConcurrentHashMap<String, Channel> userRealServerChannelMap = new ConcurrentHashMap<>();

            //开启一个线程用以发送心跳
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (userClientAlive.get()) {
                        try {
                            ProxyMessage requestMsg = new ProxyMessage(ProxyMessage.TYPE_HEARTBEAT, -1, null, null);
                            byte[] dataBytes = UdpProxyMessageCodec.encode(requestMsg);
                            socket.send(new DatagramPacket(dataBytes, dataBytes.length, new InetSocketAddress(connectInfo.userClientIp, connectInfo.userClientPort)));
                            TimeUnit.SECONDS.sleep(10);
                        } catch (InterruptedException | IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }, "userClient:" + connectInfo.userClientAddress).start();


            final byte[] byteBuf = new byte[2048];
            final DatagramPacket dataReceivedP = new DatagramPacket(byteBuf, byteBuf.length);

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
                            byte[] proxyMsgBytes = new byte[dataReceivedP.getLength()];
                            System.arraycopy(byteBuf, dataReceivedP.getOffset(), proxyMsgBytes, 0, dataReceivedP.getLength());
                            ProxyMessage msg = UdpProxyMessageCodec.decode(proxyMsgBytes);
                            if (msg.getType() == ProxyMessage.TYPE_HEARTBEAT) {
                                System.out.println("receive heartBeatInfo from " + dataReceivedP.getAddress());
                                continue;
                            }

                            logger.info("user data from :" + connectInfo.userClientAddress + " to :" + connectInfo.targetServerAddress);

                            ByteBuf buf = ByteBufAllocator.DEFAULT.buffer(2048);
                            buf.writeBytes(msg.getData(), 0, (int) msg.getSerialNumber());
                            Channel channel = userRealServerChannelMap.get(msg.getUri());
                            if (channel == null || !channel.isActive()) {
                                channel = doConnectRealServer(connectInfo.targetServerIp, connectInfo.targetServerPort, connectInfo.userClientAddress, msg.getUri());
                                userRealServerChannelMap.put(msg.getUri(), channel);
                            }
                            channel.writeAndFlush(buf);
                        } catch (SocketTimeoutException timeoutException) {
                            logger.info("user client disConnected: " + connectInfo.userClientAddress);
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

    @SneakyThrows
    public void handleUdpProxy(ChannelHandlerContext ctx, final ProxyMessage proxyMessage) {
        udpThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                handleUdpProxy(proxyMessage);
            }
        });
    }


    private boolean checkUdpPole(byte[] sendInfo, byte[] receiveInfo) {
        for (int i = 0; i < sendInfo.length; i++) {
            if (sendInfo[i] != receiveInfo[i]) return false;
        }
        return true;
    }


    @SneakyThrows
    private Channel doConnectRealServer(final String targetServerIp, final Integer targetServerPort, final String userClientAddress, final String userId) {
        ChannelFuture channelFuture = udpRealServerBootStrap.connect(targetServerIp, targetServerPort).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                if (future.isSuccess()) {
                    Channel channel = future.channel();
                    channel.attr(Constants.UDP_USER_CLIENT_IP).set(userClientAddress);
                    channel.attr(Constants.USER_ID).set(userId);
                }
            }
        });
        channelFuture.get();
        return channelFuture.channel();
    }

}

@Data
class UdpConnectInfo {
    String userClientIp;
    Integer userClientPort;
    String targetServerIp;
    Integer targetServerPort;
    String userClientAddress;
    String targetServerAddress;

    public UdpConnectInfo(ProxyMessage udpConnectInfoMsg) {

        String requestInfo = new String(udpConnectInfoMsg.getData());
        final String[] ipInfos = requestInfo.split("-");
        final String[] userClientIpInfo = ipInfos[0].split(":");
        final String[] targetServerIpInfo = ipInfos[1].split(":");

        this.userClientIp = userClientIpInfo[0];
        this.userClientPort = Integer.parseInt(userClientIpInfo[1]);

        this.targetServerIp = targetServerIpInfo[0];
        this.targetServerPort = Integer.parseInt(targetServerIpInfo[1]);

        this.userClientAddress = ipInfos[0];
        this.targetServerAddress = ipInfos[1];
    }
}
