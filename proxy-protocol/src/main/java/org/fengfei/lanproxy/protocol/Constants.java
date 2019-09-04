package org.fengfei.lanproxy.protocol;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

public interface Constants {

    public static final AttributeKey<Channel> NEXT_CHANNEL = AttributeKey.newInstance("nxt_channel");

    public static final AttributeKey<String> USER_ID = AttributeKey.newInstance("user_id");

    public static final AttributeKey<String> CLIENT_KEY = AttributeKey.newInstance("client_key");
}
