package org.fengfei.lanproxy.server;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.fengfei.lanproxy.common.container.Container;
import org.fengfei.lanproxy.protocol.ProxyMessage;
import org.fengfei.lanproxy.server.config.ProxyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.DatagramSocket;
import java.net.InetAddress;

public class UdpProxyServerContainer implements Container {
    private NioEventLoopGroup serverBossGroup;

    private static Logger logger = LoggerFactory.getLogger(UdpProxyServerContainer.class);

    public UdpProxyServerContainer() {
        this.serverBossGroup = new NioEventLoopGroup();
    }

    @Override
    public void start() {
        Bootstrap udpServerBootStrap = new Bootstrap();
        udpServerBootStrap.group(serverBossGroup).channel(NioDatagramChannel.class).handler(new ChannelInitializer<NioDatagramChannel>() {
            @Override
            protected void initChannel(NioDatagramChannel ch) throws Exception {
                ch.pipeline().addLast(new UdpHandler());
            }
        });
        try {
            udpServerBootStrap.bind(ProxyConfig.getInstance().getUdpServerBind(),
                    ProxyConfig.getInstance().getUdpServerPort()).get();
            logger.info("http udp server start on port " + ProxyConfig.getInstance().getUdpServerPort());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

    }

    @Override
    public void stop() {
        serverBossGroup.shutdownGracefully();
    }

}

class UdpHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
        InetAddress address = msg.sender().getAddress();
        int port = msg.sender().getPort();
        ByteBuf content = msg.content();
        byte[] bytes = new byte[content.readableBytes()];
        content.readBytes(bytes);
        String[] targetInfos = new String(bytes).split("-");
        String clientKey = targetInfos[0];
        String realServerAddress = targetInfos[1];


        Channel clientChannel = ProxyChannelManager.getClientChannel(clientKey);

        ProxyMessage proxyMessage = new ProxyMessage();
        proxyMessage.setData((address.getHostAddress() + ":" + port + "-" + realServerAddress).getBytes());
        proxyMessage.setType(ProxyMessage.TYPE_UDP_CONNECT);
        proxyMessage.setSerialNumber(proxyMessage.getData().length);
        proxyMessage.setUri(clientKey);
        
        clientChannel.writeAndFlush(proxyMessage);


    }
}
