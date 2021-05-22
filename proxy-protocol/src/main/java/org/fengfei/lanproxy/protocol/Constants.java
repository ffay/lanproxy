package org.fengfei.lanproxy.protocol;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

public interface Constants {

    AttributeKey<Channel> NEXT_CHANNEL = AttributeKey.newInstance("nxt_channel");

        AttributeKey<String> USER_ID = AttributeKey.newInstance("user_id");

    AttributeKey<String> CLIENT_KEY = AttributeKey.newInstance("client_key");

    AttributeKey<String> UDP_USER_CLIENT_IP = AttributeKey.newInstance("udp_user_client_ip");
}
