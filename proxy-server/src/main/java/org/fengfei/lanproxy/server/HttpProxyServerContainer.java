package org.fengfei.lanproxy.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.fengfei.lanproxy.common.container.Container;
import org.fengfei.lanproxy.server.config.ProxyConfig;
import org.fengfei.lanproxy.server.handlers.UserChannelHttpHandler;
import org.fengfei.lanproxy.server.metrics.handler.BytesMetricsHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpProxyServerContainer implements Container {


    private static Logger logger = LoggerFactory.getLogger(HttpProxyServerContainer.class);

    private NioEventLoopGroup serverWorkerGroup;

    private NioEventLoopGroup serverBossGroup;


    public HttpProxyServerContainer() {

        serverBossGroup = new NioEventLoopGroup();
        serverWorkerGroup = new NioEventLoopGroup();

    }

    @Override
    public void start() {
        ServerBootstrap httpServerBootstrap = new ServerBootstrap();
        httpServerBootstrap.group(serverBossGroup, serverWorkerGroup).channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        ch.pipeline().addFirst(new BytesMetricsHandler());
                        pipeline.addLast(new UserChannelHttpHandler());
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
