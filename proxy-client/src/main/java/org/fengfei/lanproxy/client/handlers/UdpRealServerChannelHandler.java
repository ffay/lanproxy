package org.fengfei.lanproxy.client.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.EventExecutorGroup;
import org.fengfei.lanproxy.protocol.Constants;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

/**
 * @author ZhangJiaKui
 * @classname UdpRealServerChannelHandler
 * @description TODO
 * @date 5/21/2021 11:39 AM
 */
public class UdpRealServerChannelHandler extends SimpleChannelInboundHandler<ByteBuf> {


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        DatagramSocket socket = new DatagramSocket();
        String clientUdpAddress = ctx.channel().attr(Constants.UDP_USER_CLIENT_IP).get();
        String[] ipInfos = clientUdpAddress.split(":");
        byte[] bytes = new byte[msg.readableBytes()];
        msg.readBytes(bytes);
        socket.send(new DatagramPacket(bytes, bytes.length, new InetSocketAddress(ipInfos[0], Integer.parseInt(ipInfos[1]))));
    }

}
