package org.fengfei.lanproxy.server.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import org.fengfei.lanproxy.protocol.Constants;
import org.fengfei.lanproxy.protocol.ProxyMessage;
import org.fengfei.lanproxy.server.HttpProxyServerContainer;
import org.fengfei.lanproxy.server.ProxyChannelManager;
import org.fengfei.lanproxy.server.config.web.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 服务端使用http代理的 请求方式,使用url 替代 具体端口
 */
public class UserChannelHttpHandler2 extends SimpleChannelInboundHandler<ByteBuf> {
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


    public UserChannelHttpHandler2() {
        super();
    }

    public UserChannelHttpHandler2(ProxyChannelManager proxyChannelManager) {
        this.proxyChannelManager = proxyChannelManager;
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf byteBuf) throws Exception {
//        String uri = httpRequest.getUri();
//        String clientKey = uri.split("/")[1];
//        Channel clientChannel = ProxyChannelManager.getClientChannel(clientKey);
//        if (clientChannel == null) {
//            ctx.channel().close();
//            return;
//        } else if (!connectedClientChannel.contains(clientChannel)) {
//            Channel httpProxyChannel = ctx.channel();
//            connectedClientChannel.add(clientChannel);
//            String userId = "zjkxx";
//            InetSocketAddress sa = (InetSocketAddress) httpProxyChannel.localAddress();
//            String lanInfo = "127.0.0.1:8000";
//            // 用户连接到代理服务器时，设置用户连接不可读，等待代理后端服务器连接成功后再改变为可读状态
//            httpProxyChannel.config().setOption(ChannelOption.AUTO_READ, false);
//            ProxyChannelManager.addUserChannelToCmdChannel(clientChannel, userId, httpProxyChannel);
//            ProxyMessage proxyMessage = new ProxyMessage();
//            proxyMessage.setType(ProxyMessage.TYPE_CONNECT);
//            proxyMessage.setUri(userId);
//            proxyMessage.setData(lanInfo.getBytes());
//            clientChannel.writeAndFlush(proxyMessage);
//            logger.info("连接代理客户端");
//        }
//        TimeUnit.SECONDS.sleep(5);
//        ByteBuf byteBuf = clientChannel.alloc().buffer();
//        HttpRequestEncoder encoder = new HttpRequestEncoder();
////        doCall(encoder, "encodeInitialLine", byteBuf, httpRequest);
//        String value = "GET / HTTP/1.1\n" +
//                "Host: 127.0.0.1:9920\n" +
//                "Connection: keep-alive\n" +
//                "Cache-Control: max-age=0\n" +
//                "Upgrade-Insecure-Requests: 1\n" +
//                "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.128 Safari/537.36 Edg/89.0.774.77\n" +
//                "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9\n" +
//                "Sec-Fetch-Site: none\n" +
//                "Sec-Fetch-Mode: navigate\n" +
//                "Sec-Fetch-User: ?1\n" +
//                "Sec-Fetch-Dest: document\n" +
//                "Accept-Encoding: gzip, deflate, br\n" +
//                "Accept-Language: zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6\n" +
//                "Cookie: token=893b149f508f48b0b971c9b38cda0b2d\n";
//        byte[] bytes = value.getBytes();
//
//        ProxyMessage proxyMessage = new ProxyMessage();
//        proxyMessage.setType(ProxyMessage.P_TYPE_TRANSFER);
//        proxyMessage.setUri("zjkxx");
//        proxyMessage.setData(bytes);
//        clientChannel.writeAndFlush(proxyMessage);

        byte[] bytes = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(bytes);
        // 通知代理客户端
        Channel userChannel = ctx.channel();
        Channel proxyChannel = userChannel.attr(Constants.NEXT_CHANNEL).get();
        if (proxyChannel == null) {

            // 该端口还没有代理客户端
            ctx.channel().close();
        } else {
            String userId = ProxyChannelManager.getUserChannelUserId(userChannel);
            ProxyMessage proxyMessage = new ProxyMessage();
            proxyMessage.setType(ProxyMessage.P_TYPE_TRANSFER);
            proxyMessage.setUri(userId);
            proxyMessage.setData(bytes);
            proxyChannel.writeAndFlush(proxyMessage);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel clientChannel = ProxyChannelManager.getClientChannel("82e3e7294cfc41b49cbc1a906646e050");

        Channel userChannel = ctx.channel();

        if (clientChannel == null) {
            // 该端口还没有代理客户端
            ctx.channel().close();
        } else {
            String userId = newUserId();
            String lanInfo = "127.0.0.1:8000";
            // 用户连接到代理服务器时，设置用户连接不可读，等待代理后端服务器连接成功后再改变为可读状态
            userChannel.config().setOption(ChannelOption.AUTO_READ, false);
            ProxyChannelManager.addUserChannelToCmdChannel(clientChannel, userId, userChannel);
            ProxyMessage proxyMessage = new ProxyMessage();
            proxyMessage.setType(ProxyMessage.TYPE_CONNECT);
            proxyMessage.setUri(userId);
            proxyMessage.setData(lanInfo.getBytes());
            clientChannel.writeAndFlush(proxyMessage);
        }

        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {

        // 通知代理客户端
        Channel userChannel = ctx.channel();
        InetSocketAddress sa = (InetSocketAddress) userChannel.localAddress();
        Channel cmdChannel = ProxyChannelManager.getClientChannel(sa.getPort());
        if (cmdChannel == null) {

            // 该端口还没有代理客户端
            ctx.channel().close();
        } else {

            // 用户连接断开，从控制连接中移除
            String userId = ProxyChannelManager.getUserChannelUserId(userChannel);
            ProxyChannelManager.removeUserChannelFromCmdChannel(cmdChannel, userId);
            Channel proxyChannel = userChannel.attr(Constants.NEXT_CHANNEL).get();
            if (proxyChannel != null && proxyChannel.isActive()) {
                proxyChannel.attr(Constants.NEXT_CHANNEL).remove();
                proxyChannel.attr(Constants.CLIENT_KEY).remove();
                proxyChannel.attr(Constants.USER_ID).remove();

                proxyChannel.config().setOption(ChannelOption.AUTO_READ, true);
                // 通知客户端，用户连接已经断开
                ProxyMessage proxyMessage = new ProxyMessage();
                proxyMessage.setType(ProxyMessage.TYPE_DISCONNECT);
                proxyMessage.setUri(userId);
                proxyChannel.writeAndFlush(proxyMessage);
            }
        }

        super.channelInactive(ctx);
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {

        // 通知代理客户端
        Channel userChannel = ctx.channel();
        InetSocketAddress sa = (InetSocketAddress) userChannel.localAddress();
        Channel cmdChannel = ProxyChannelManager.getClientChannel(sa.getPort());
        if (cmdChannel == null) {

            // 该端口还没有代理客户端
            ctx.channel().close();
        } else {
            Channel proxyChannel = userChannel.attr(Constants.NEXT_CHANNEL).get();
            if (proxyChannel != null) {
                proxyChannel.config().setOption(ChannelOption.AUTO_READ, userChannel.isWritable());
            }
        }

        super.channelWritabilityChanged(ctx);
    }

    private static String newUserId() {
        return String.valueOf(userIdProducer.incrementAndGet());
    }
}
