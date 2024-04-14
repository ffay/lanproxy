package cn.dxbtech.portbridge.client;

import cn.dxbtech.portbridge.client.handlers.ClientChannelHandler;
import cn.dxbtech.portbridge.client.handlers.RealServerChannelHandler;
import cn.dxbtech.portbridge.client.listener.ChannelStatusListener;
import cn.dxbtech.portbridge.commons.CommandLineUtil;
import cn.dxbtech.portbridge.commons.Config;
import cn.dxbtech.portbridge.commons.container.Container;
import cn.dxbtech.portbridge.commons.container.ContainerHelper;
import cn.dxbtech.portbridge.protocol.IdleCheckHandler;
import cn.dxbtech.portbridge.protocol.ProxyMessage;
import cn.dxbtech.portbridge.protocol.ProxyMessageDecoder;
import cn.dxbtech.portbridge.protocol.ProxyMessageEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslHandler;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class ProxyClientContainer implements Container, ChannelStatusListener {

    private static Logger logger = LoggerFactory.getLogger(ProxyClientContainer.class);

    private static final int MAX_FRAME_LENGTH = 1024 * 1024;

    private static final int LENGTH_FIELD_OFFSET = 0;

    private static final int LENGTH_FIELD_LENGTH = 4;

    private static final int INITIAL_BYTES_TO_STRIP = 0;

    private static final int LENGTH_ADJUSTMENT = 0;

    private NioEventLoopGroup workerGroup;

    private Bootstrap bootstrap;

    private Bootstrap realServerBootstrap;

    private Config config = Config.getInstance();

    private SSLContext sslContext;

    private long sleepTimeMill = 1000;

    public ProxyClientContainer() {
        workerGroup = new NioEventLoopGroup();
        realServerBootstrap = new Bootstrap();
        realServerBootstrap.group(workerGroup);
        realServerBootstrap.channel(NioSocketChannel.class);
        realServerBootstrap.handler(new ChannelInitializer<SocketChannel>() {

            @Override
            public void initChannel(SocketChannel ch) {
                ch.pipeline().addLast(new RealServerChannelHandler());
            }
        });

        bootstrap = new Bootstrap();
        bootstrap.group(workerGroup);
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {

            @Override
            public void initChannel(SocketChannel ch) {
                if (Config.getInstance().getBooleanValue("ssl.enable", false)) {
                    if (sslContext == null) {
                        sslContext = SslContextCreator.createSSLContext();
                    }

                    ch.pipeline().addLast(createSslHandler(sslContext));
                }
                ch.pipeline().addLast(new ProxyMessageDecoder(MAX_FRAME_LENGTH, LENGTH_FIELD_OFFSET, LENGTH_FIELD_LENGTH, LENGTH_ADJUSTMENT, INITIAL_BYTES_TO_STRIP));
                ch.pipeline().addLast(new ProxyMessageEncoder());
                ch.pipeline().addLast(new IdleCheckHandler(IdleCheckHandler.READ_IDLE_TIME, IdleCheckHandler.WRITE_IDLE_TIME - 10, 0));
                ch.pipeline().addLast(new ClientChannelHandler(realServerBootstrap, bootstrap, ProxyClientContainer.this));
            }
        });
    }

    @Override
    public void start() {
        connectProxyServer();
    }

    private ChannelHandler createSslHandler(SSLContext sslContext) {
        SSLEngine sslEngine = sslContext.createSSLEngine();
        sslEngine.setUseClientMode(true);
        return new SslHandler(sslEngine);
    }

    private void connectProxyServer() {

        bootstrap.connect(config.getStringValue("server.host"), config.getIntValue("server.port")).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {

                // 连接成功，向服务器发送客户端认证信息（clientKey）
                ClientChannelMannager.setCmdChannel(future.channel());
                ProxyMessage proxyMessage = new ProxyMessage();
                proxyMessage.setType(ProxyMessage.C_TYPE_AUTH);
                proxyMessage.setUri(config.getStringValue("client.key"));
                future.channel().writeAndFlush(proxyMessage);
                sleepTimeMill = 1000;
                logger.info("connect proxy server success, {}", future.channel());
            } else {
                logger.warn("connect proxy server failed", future.cause());

                // 连接失败，发起重连
                reconnectWait();
                connectProxyServer();
            }
        });
    }

    @Override
    public void stop() {
        workerGroup.shutdownGracefully();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        reconnectWait();
        connectProxyServer();
    }

    private void reconnectWait() {
        try {
            if (sleepTimeMill > 60000) {
                sleepTimeMill = 1000;
            }

            synchronized (this) {
                sleepTimeMill = sleepTimeMill * 2;
                wait(sleepTimeMill);
            }
        } catch (InterruptedException ignore) {
        }
    }

    public static void main(String[] args) {

        CommandLineUtil commandLineUtil = new CommandLineUtil(args);

        if (commandLineUtil.containsKey("-h") || commandLineUtil.containsKey("--help")) {
            commandLineUtil.addHelp("-c", "close after", "Run a while if configuared, 1d/2h/3m");
            commandLineUtil.addHelp("-l", "log4j.properties", "Specific log4j.properties file location.");
            commandLineUtil.addHelp("-s", "server.host", "Server host address/ip.");
            commandLineUtil.addHelp("-p", "server.port", "Server host port.");
            commandLineUtil.addHelp("-k", "client.key", "Client key shows in admin web of this client.");
            commandLineUtil.addHelp("-h/--help", "help", "Print help information.");

            logger.info("{}", commandLineUtil.help());
            return;
        }

        if (commandLineUtil.contains("-c")) {
            String time = commandLineUtil.get("-c");
            long second;
            if (time.matches("^\\d+$")) {
                second = Long.parseLong(time);
            } else if (time.matches("^\\d+d$")) {
                second = TimeUnit.DAYS.toSeconds(Long.parseLong(time.replace("d", "")));
            } else if (time.matches("^\\d+h$")) {
                second = TimeUnit.HOURS.toSeconds(Long.parseLong(time.replace("h", "")));
            } else if (time.matches("^\\d+m$")) {
                second = TimeUnit.MINUTES.toSeconds(Long.parseLong(time.replace("m", "")));
            } else {
                throw new IllegalArgumentException("Unsupported -c config " + time);
            }
            logger.info("Progress will close after {} seconds", second);
            new Thread(() -> {
                try {
                    TimeUnit.SECONDS.sleep(second);
                    System.exit(0);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }

        if (commandLineUtil.contains("-l")) {
            PropertyConfigurator.configure(commandLineUtil.get("-l"));
        }

//        -s SERVER_IP -p SERVER_PORT -k CLIENT_KEY
        Properties properties = new Properties();
        if (commandLineUtil.contains("-s")) {
            properties.setProperty("server.host", commandLineUtil.get("-s"));
        }
        if (commandLineUtil.contains("-p")) {
            properties.setProperty("server.port", commandLineUtil.get("-p"));
        }
        if (commandLineUtil.contains("-k")) {
            properties.setProperty("client.key", commandLineUtil.get("-k"));
        }
        if (commandLineUtil.contains("-ssl")) {
            properties.setProperty("ssl.enable", commandLineUtil.get("-ssl"));
            if (commandLineUtil.contains("-ssl_jks")) {
                properties.setProperty("ssl.jksPath", commandLineUtil.get("-ssl_jks"));
            }
            if (commandLineUtil.contains("-ssl_ks_pwd")) {
                properties.setProperty("ssl.keyStorePassword", commandLineUtil.get("-ssl_ks_pwd"));
            }
        }

        logger.info("command line properties: {}", properties.entrySet());
        Config.getInstance().loadProperties(properties);

        ContainerHelper.start(Arrays.asList(new Container[]{new ProxyClientContainer()}));
    }

}
