package org.fengfei.lanproxy.server.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpRequestEncoder;
import org.fengfei.lanproxy.protocol.ProxyMessage;
import org.fengfei.lanproxy.server.HttpProxyServerContainer;
import org.fengfei.lanproxy.server.ProxyChannelManager;
import org.fengfei.lanproxy.server.config.ProxyConfig;
import org.fengfei.lanproxy.server.config.web.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 服务端使用http代理的 请求方式,使用url 替代 具体端口
 */
public class UserChannelHttpHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static Logger logger = LoggerFactory.getLogger(HttpProxyServerContainer.class);

    private static AtomicLong userIdProducer = new AtomicLong(0);


    private static final int MAX_FRAME_LENGTH = 2 * 1024 * 1024;

    private static final int LENGTH_FIELD_OFFSET = 0;

    private static final int LENGTH_FIELD_LENGTH = 4;

    private static final int INITIAL_BYTES_TO_STRIP = 0;

    private static final int LENGTH_ADJUSTMENT = 0;

    private static Map<String, RequestHandler> routes = new ConcurrentHashMap<String, RequestHandler>();

    private final static Set<Channel> connectedClientChannel = new HashSet<>();

    private ProxyChannelManager proxyChannelManager;


    public UserChannelHttpHandler() {
        super();
    }

    public UserChannelHttpHandler(ProxyChannelManager proxyChannelManager) {
        this.proxyChannelManager = proxyChannelManager;
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest httpRequest) throws Exception {
        String uri = httpRequest.getUri();
        String clientKey = uri.split("/")[1];
        Channel clientChannel = ProxyChannelManager.getClientChannel(clientKey);
        if (clientChannel == null) {
            ctx.channel().close();
            return;
        } else if (!connectedClientChannel.contains(clientChannel)) {
            Channel httpProxyChannel = ctx.channel();
            connectedClientChannel.add(clientChannel);
            String userId = "zjkxx";
            InetSocketAddress sa = (InetSocketAddress) httpProxyChannel.localAddress();
            String lanInfo = "127.0.0.1:8000";
            // 用户连接到代理服务器时，设置用户连接不可读，等待代理后端服务器连接成功后再改变为可读状态
            httpProxyChannel.config().setOption(ChannelOption.AUTO_READ, false);
            ProxyChannelManager.addUserChannelToCmdChannel(clientChannel, userId, httpProxyChannel);
            ProxyMessage proxyMessage = new ProxyMessage();
            proxyMessage.setType(ProxyMessage.TYPE_CONNECT);
            proxyMessage.setUri(userId);
            proxyMessage.setData(lanInfo.getBytes());
            clientChannel.writeAndFlush(proxyMessage);
            logger.info("连接代理客户端");
        }
        TimeUnit.SECONDS.sleep(5);
        ByteBuf byteBuf = clientChannel.alloc().buffer();
        HttpRequestEncoder encoder = new HttpRequestEncoder();
//        doCall(encoder, "encodeInitialLine", byteBuf, httpRequest);
        String value = "GET / HTTP/1.1\n" +
                "Host: 127.0.0.1:9920\n" +
                "Connection: keep-alive\n" +
                "Cache-Control: max-age=0\n" +
                "Upgrade-Insecure-Requests: 1\n" +
                "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.128 Safari/537.36 Edg/89.0.774.77\n" +
                "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9\n" +
                "Sec-Fetch-Site: none\n" +
                "Sec-Fetch-Mode: navigate\n" +
                "Sec-Fetch-User: ?1\n" +
                "Sec-Fetch-Dest: document\n" +
                "Accept-Encoding: gzip, deflate, br\n" +
                "Accept-Language: zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6\n" +
                "Cookie: token=893b149f508f48b0b971c9b38cda0b2d\n";
        byte[] bytes = value.getBytes();

        ProxyMessage proxyMessage = new ProxyMessage();
        proxyMessage.setType(ProxyMessage.P_TYPE_TRANSFER);
        proxyMessage.setUri("zjkxx");
        proxyMessage.setData(bytes);
        clientChannel.writeAndFlush(proxyMessage);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        super.channelActive(ctx);
    }

    static void doCall(Object o, String methodName, Object... args) {
        Class<?> c = o.getClass();
        for (Method method : c.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                try {
                    method.invoke(o, args);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return;
            }
        }


    }

    private static String newUserId() {
        return String.valueOf(userIdProducer.incrementAndGet());
    }
}
