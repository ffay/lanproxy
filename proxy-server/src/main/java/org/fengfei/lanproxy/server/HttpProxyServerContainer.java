package org.fengfei.lanproxy.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.fengfei.lanproxy.common.Config;
import org.fengfei.lanproxy.common.container.Container;
import org.fengfei.lanproxy.common.container.ContainerHelper;
import org.fengfei.lanproxy.protocol.IdleCheckHandler;
import org.fengfei.lanproxy.protocol.ProxyMessageDecoder;
import org.fengfei.lanproxy.protocol.ProxyMessageEncoder;
import org.fengfei.lanproxy.server.config.ProxyConfig;
import org.fengfei.lanproxy.server.config.ProxyConfig.ConfigChangedListener;
import org.fengfei.lanproxy.server.config.web.HttpRequestHandler;
import org.fengfei.lanproxy.server.config.web.WebConfigContainer;
import org.fengfei.lanproxy.server.config.web.routes.RouteConfig;
import org.fengfei.lanproxy.server.handlers.ServerChannelHandler;
import org.fengfei.lanproxy.server.handlers.UserChannelHandler;
import org.fengfei.lanproxy.server.handlers.UserChannelHttpHandler;
import org.fengfei.lanproxy.server.metrics.handler.BytesMetricsHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.net.BindException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HttpProxyServerContainer implements Container {

    /**
     * max packet is 2M.
     */
    private static final int MAX_FRAME_LENGTH = 2 * 1024 * 1024;

    private static final int LENGTH_FIELD_OFFSET = 0;

    private static final int LENGTH_FIELD_LENGTH = 4;

    private static final int INITIAL_BYTES_TO_STRIP = 0;

    private static final int LENGTH_ADJUSTMENT = 0;

    private static Logger logger = LoggerFactory.getLogger(HttpProxyServerContainer.class);

    private NioEventLoopGroup serverWorkerGroup;

    private NioEventLoopGroup serverBossGroup;

    private HttpProxyServerContainer serverContainerSelf;

    private static final Map<String, List<Channel>> clientKeyAndUserPortBindChannelsMap = new ConcurrentHashMap<>();
    private static final Map<Channel, List<Channel>> clientChannelAndUserPortBindChannelsMap = new ConcurrentHashMap<>();

    public HttpProxyServerContainer() {

        serverBossGroup = new NioEventLoopGroup();
        serverWorkerGroup = new NioEventLoopGroup();
        this.serverContainerSelf = this;

    }

    @Override
    public void start() {
        ServerBootstrap httpServerBootstrap = new ServerBootstrap();
        httpServerBootstrap.group(serverBossGroup, serverWorkerGroup).channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new HttpServerCodec());
                        pipeline.addLast(new HttpObjectAggregator(64 * 1024));
                        pipeline.addLast(new ChunkedWriteHandler());
                        pipeline.addLast(new UserChannelHttpHandler());
//                        ch.pipeline().addFirst(new BytesMetricsHandler());
//                        ch.pipeline().addLast(new UserChannelHandler());
                    }
                });

        try {
            httpServerBootstrap.bind(ProxyConfig.getInstance().getHttpServerProxyBind(),
                    ProxyConfig.getInstance().getHttpServerProxyPort()).get();
            logger.info("http proxy server start on port " + ProxyConfig.getInstance().getHttpServerProxyPort());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }


    @Override
    public void stop() {
        serverBossGroup.shutdownGracefully();
        serverWorkerGroup.shutdownGracefully();
    }


}
