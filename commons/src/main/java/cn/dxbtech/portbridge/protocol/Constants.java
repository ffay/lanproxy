package cn.dxbtech.portbridge.protocol;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

public interface Constants {

    AttributeKey<Channel> NEXT_CHANNEL = AttributeKey.newInstance("nxt_channel");

    AttributeKey<String> USER_ID = AttributeKey.newInstance("user_id");

    AttributeKey<String> CLIENT_KEY = AttributeKey.newInstance("client_key");
}
