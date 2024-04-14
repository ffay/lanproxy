package cn.dxbtech.portbridge.client.listener;

import io.netty.channel.ChannelHandlerContext;

public interface ChannelStatusListener {

    void channelInactive(ChannelHandlerContext ctx);

}
