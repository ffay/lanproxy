package org.fengfei.lanproxy.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslHandler;
import org.fengfei.lanproxy.common.Config;
import org.fengfei.lanproxy.common.container.Container;
import org.fengfei.lanproxy.common.container.ContainerHelper;
import org.fengfei.lanproxy.protocol.IdleCheckHandler;
import org.fengfei.lanproxy.protocol.ProxyMessageDecoder;
import org.fengfei.lanproxy.protocol.ProxyMessageEncoder;
import org.fengfei.lanproxy.server.config.ProxyConfig;
import org.fengfei.lanproxy.server.config.ProxyConfig.ConfigChangedListener;
import org.fengfei.lanproxy.server.config.web.WebConfigContainer;
import org.fengfei.lanproxy.server.handlers.ServerChannelHandler;
import org.fengfei.lanproxy.server.handlers.UserChannelHandler;
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

public class ProxyServerContainer implements Container, ConfigChangedListener {

    /**
     * max packet is 2M.
     */
    private static final int MAX_FRAME_LENGTH = 2 * 1024 * 1024;

    private static final int LENGTH_FIELD_OFFSET = 0;

    private static final int LENGTH_FIELD_LENGTH = 4;

    private static final int INITIAL_BYTES_TO_STRIP = 0;

    private static final int LENGTH_ADJUSTMENT = 0;

    private static Logger logger = LoggerFactory.getLogger(ProxyServerContainer.class);

    private NioEventLoopGroup serverWorkerGroup;

    private NioEventLoopGroup serverBossGroup;

    private ProxyServerContainer serverContainerSelf;

    private static final Map<String, List<Channel>> clientKeyAndUserPortBindChannelsMap = new ConcurrentHashMap<>();
    private static final Map<Channel, List<Channel>> clientChannelAndUserPortBindChannelsMap = new ConcurrentHashMap<>();

    public ProxyServerContainer() {

        serverBossGroup = new NioEventLoopGroup();
        serverWorkerGroup = new NioEventLoopGroup();
        this.serverContainerSelf = this;
        ProxyConfig.getInstance().addConfigChangedListener(this);

    }

    @Override
    public void start() {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(serverBossGroup, serverWorkerGroup).channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<SocketChannel>() {

            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new ProxyMessageDecoder(MAX_FRAME_LENGTH, LENGTH_FIELD_OFFSET, LENGTH_FIELD_LENGTH, LENGTH_ADJUSTMENT, INITIAL_BYTES_TO_STRIP));
                ch.pipeline().addLast(new ProxyMessageEncoder());
                ch.pipeline().addLast(new IdleCheckHandler(IdleCheckHandler.READ_IDLE_TIME, IdleCheckHandler.WRITE_IDLE_TIME, 0));
                ch.pipeline().addLast(new ServerChannelHandler(serverContainerSelf));
            }
        });

        try {
            bootstrap.bind(ProxyConfig.getInstance().getServerBind(), ProxyConfig.getInstance().getServerPort()).get();
            logger.info("proxy server start on port " + ProxyConfig.getInstance().getServerPort());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        if (Config.getInstance().getBooleanValue("server.ssl.enable", false)) {
            String host = Config.getInstance().getStringValue("server.ssl.bind", "0.0.0.0");
            int port = Config.getInstance().getIntValue("server.ssl.port");
            initializeSSLTCPTransport(host, port, new SslContextCreator().initSSLContext());
        }
    }

    private void initializeSSLTCPTransport(String host, int port, final SSLContext sslContext) {
        ServerBootstrap b = new ServerBootstrap();
        b.group(serverBossGroup, serverWorkerGroup).channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<SocketChannel>() {

            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                try {
                    pipeline.addLast("ssl", createSslHandler(sslContext, Config.getInstance().getBooleanValue("server.ssl.needsClientAuth", false)));
                    ch.pipeline().addLast(new ProxyMessageDecoder(MAX_FRAME_LENGTH, LENGTH_FIELD_OFFSET, LENGTH_FIELD_LENGTH, LENGTH_ADJUSTMENT, INITIAL_BYTES_TO_STRIP));
                    ch.pipeline().addLast(new ProxyMessageEncoder());
                    ch.pipeline().addLast(new IdleCheckHandler(IdleCheckHandler.READ_IDLE_TIME, IdleCheckHandler.WRITE_IDLE_TIME, 0));
                    ch.pipeline().addLast(new ServerChannelHandler());
                } catch (Throwable th) {
                    logger.error("Severe error during pipeline creation", th);
                    throw th;
                }
            }
        });
        try {

            // Bind and start to accept incoming connections.
            ChannelFuture f = b.bind(host, port);
            f.sync();
            logger.info("proxy ssl server start on port {}", port);
        } catch (InterruptedException ex) {
            logger.error("An interruptedException was caught while initializing server", ex);
        }
    }

    private void startUserPort() {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(serverBossGroup, serverWorkerGroup).channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<SocketChannel>() {

            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addFirst(new BytesMetricsHandler());
                ch.pipeline().addLast(new UserChannelHandler());
            }
        });

        List<Integer> ports = ProxyConfig.getInstance().getUserPorts();
        for (int port : ports) {
            try {
                bootstrap.bind(port).get();
                logger.info("bind user port " + port);
            } catch (Exception ex) {

                // BindException表示该端口已经绑定过
                if (!(ex.getCause() instanceof BindException)) {
                    throw new RuntimeException(ex);
                }
            }
        }


    }

    /**
     * 开启对应客户端需要的port
     *
     * @param ports
     */
    public void startClientPorts(String clientKey, List<Integer> ports, Channel clientChannel) {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(serverBossGroup, serverWorkerGroup).channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<SocketChannel>() {

            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addFirst(new BytesMetricsHandler());
                ch.pipeline().addLast(new UserChannelHandler());
            }
        });
        ArrayList<Channel> channels = new ArrayList<>();

        for (int port : ports) {
            try {
                ChannelFuture sync = bootstrap.bind(port).sync();
                channels.add(sync.channel());
                sync.get();
                logger.info("bind user port " + port);
            } catch (Exception ex) {

                // BindException表示该端口已经绑定过
                if (!(ex.getCause() instanceof BindException)) {
                    throw new RuntimeException(ex);
                }
            }
        }
        clientKeyAndUserPortBindChannelsMap.put(clientKey, channels);
        clientChannelAndUserPortBindChannelsMap.put(clientChannel, channels);
    }

    /**
     * 关闭对应的端口
     *
     * @param clientKey
     */
    public void closeClientPorts(String clientKey) throws InterruptedException {
        for (Channel channel : clientKeyAndUserPortBindChannelsMap.get(clientKey)) {
            logger.info(String.format("正在关闭%s客户端 所需公网端口 %s", clientKey, channel.remoteAddress()));
            channel.close().sync();
        }
    }

    public void closeClientPorts(Channel clientChannel) throws InterruptedException {
        for (Channel channel : clientChannelAndUserPortBindChannelsMap.get(clientChannel)) {
            logger.info(String.format("正在关闭%s客户端 所需公网端口 %s", clientChannel.remoteAddress(), channel.localAddress()));
            channel.close().sync();
        }
    }

    @Override
    public void onChanged() {
        startUserPort();
    }

    @Override
    public void stop() {
        serverBossGroup.shutdownGracefully();
        serverWorkerGroup.shutdownGracefully();
    }

    private ChannelHandler createSslHandler(SSLContext sslContext, boolean needsClientAuth) {
        SSLEngine sslEngine = sslContext.createSSLEngine();
        sslEngine.setUseClientMode(false);
        if (needsClientAuth) {
            sslEngine.setNeedClientAuth(true);
        }

        return new SslHandler(sslEngine);
    }

    public static void main(String[] args) {
        //proxyServer 代理转发  WebConfig web后台处理
        ContainerHelper.start(Arrays.asList(new ProxyServerContainer(), new WebConfigContainer(), new HttpProxyServerContainer(), new UdpProxyServerContainer()));
    }


}
