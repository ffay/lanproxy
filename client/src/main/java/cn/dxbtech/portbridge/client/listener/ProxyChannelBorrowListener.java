package cn.dxbtech.portbridge.client.listener;

import io.netty.channel.Channel;

public interface ProxyChannelBorrowListener {

    void success(Channel channel);

    void error(Throwable cause);

}
