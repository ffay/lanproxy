package org.fengfei.lanproxy.server;

import java.net.BindException;
import java.util.Arrays;
import java.util.List;

import org.fengfei.lanproxy.common.container.Container;
import org.fengfei.lanproxy.common.container.ContainerHelper;
import org.fengfei.lanproxy.protocol.IdleCheckHandler;
import org.fengfei.lanproxy.protocol.ProxyMessageDecoder;
import org.fengfei.lanproxy.protocol.ProxyMessageEncoder;
import org.fengfei.lanproxy.server.config.ProxyConfig;
import org.fengfei.lanproxy.server.config.ProxyConfig.ConfigChangedListener;
import org.fengfei.lanproxy.server.handlers.ServerChannelHandler;
import org.fengfei.lanproxy.server.handlers.UserChannelHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

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

    public ProxyServerContainer() {

        serverBossGroup = new NioEventLoopGroup();
        serverWorkerGroup = new NioEventLoopGroup();

        ProxyConfig.addConfigChangedListener(this);
        ProxyConfig.update();

    }

    @Override
    public void start() {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(serverBossGroup, serverWorkerGroup).channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {

                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new ProxyMessageDecoder(MAX_FRAME_LENGTH, LENGTH_FIELD_OFFSET,
                                LENGTH_FIELD_LENGTH, LENGTH_ADJUSTMENT, INITIAL_BYTES_TO_STRIP));
                        ch.pipeline().addLast(new ProxyMessageEncoder());
                        ch.pipeline().addLast(new IdleCheckHandler(IdleCheckHandler.READ_IDLE_TIME,
                                IdleCheckHandler.WRITE_IDLE_TIME, 0));
                        ch.pipeline().addLast(new ServerChannelHandler());
                    }
                });

        try {
            bootstrap.bind(ProxyConfig.getInstance().getServerPort()).get();
            logger.info("proxy server start on port " + ProxyConfig.getInstance().getServerPort());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

    }

    private void startUserPort() {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(serverBossGroup, serverWorkerGroup).channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {

                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new IdleCheckHandler(IdleCheckHandler.USER_CHANNEL_READ_IDLE_TIME, 0, 0));
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

    @Override
    public void onChanged() {
        startUserPort();
    }

    @Override
    public void stop() {
        serverBossGroup.shutdownGracefully();
        serverWorkerGroup.shutdownGracefully();
    }

    public static void main(String[] args) {
        ContainerHelper.start(Arrays.asList(new Container[] { new ProxyServerContainer() }));
    }

}
