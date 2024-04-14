package cn.dxbtech.portbridge.server;

import cn.dxbtech.portbridge.commons.CommandLineUtil;
import cn.dxbtech.portbridge.commons.Config;
import cn.dxbtech.portbridge.commons.container.Container;
import cn.dxbtech.portbridge.commons.container.ContainerHelper;
import cn.dxbtech.portbridge.protocol.IdleCheckHandler;
import cn.dxbtech.portbridge.protocol.ProxyMessageDecoder;
import cn.dxbtech.portbridge.protocol.ProxyMessageEncoder;
import cn.dxbtech.portbridge.server.config.ProxyConfig;
import cn.dxbtech.portbridge.server.config.web.WebConfigContainer;
import cn.dxbtech.portbridge.server.handlers.ServerChannelHandler;
import cn.dxbtech.portbridge.server.handlers.UserChannelHandler;
import cn.dxbtech.portbridge.server.metrics.MetricsPersistenceWrapper;
import cn.dxbtech.portbridge.server.metrics.handler.BytesMetricsHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslHandler;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.net.BindException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.Manifest;

public class ProxyServerContainer implements Container, ProxyConfig.ConfigChangedListener {

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

        ProxyConfig.getInstance().addConfigChangedListener(this);
    }

    public static void main(String[] args) {
        CommandLineUtil commandLineUtil = new CommandLineUtil(args);

        if (commandLineUtil.containsKey("-h") || commandLineUtil.containsKey("--help")) {
            commandLineUtil.addHelp("-l", "log4j.properties", "Specific log4j.properties file location.");
            commandLineUtil.addHelp("-s", "server.bind", "Bind address of server.");
            commandLineUtil.addHelp("-p", "server.port", "Bind port of server.");
            commandLineUtil.addHelp("-cs", "config.server.bind", "Bind web config address of server.");
            commandLineUtil.addHelp("-cp", "config.server.port", "Bind web config port of server.");
            commandLineUtil.addHelp("-u", "config.admin.username", "Specific web config username.");
            commandLineUtil.addHelp("-pw", "config.admin.password", "Specific web config password.");
            commandLineUtil.addHelp("-h/--help", "help", "Print help information.");

            logger.info("{}", commandLineUtil.help());
            return;
        }

        if (commandLineUtil.contains("-l")) {
            PropertyConfigurator.configure(commandLineUtil.get("-l"));
        }

        Properties properties = new Properties();
        if (commandLineUtil.contains("-s")) {
            properties.setProperty("server.bind", commandLineUtil.get("-s"));
        }
        if (commandLineUtil.contains("-p")) {
            properties.setProperty("server.port", commandLineUtil.get("-p"));
        }
        if (commandLineUtil.contains("-cs")) {
            properties.setProperty("config.server.bind", commandLineUtil.get("-cs"));
        }
        if (commandLineUtil.contains("-cp")) {
            properties.setProperty("config.server.port", commandLineUtil.get("-cp"));
        }
        if (commandLineUtil.contains("-u")) {
            properties.setProperty("config.admin.username", commandLineUtil.get("-u"));
        }
        if (commandLineUtil.contains("-pw")) {
            properties.setProperty("config.admin.password", commandLineUtil.get("-pw"));
        }
        if (commandLineUtil.contains("-ssl")) {
            properties.setProperty("server.ssl.enable", commandLineUtil.get("-ssl"));
            properties.setProperty("server.ssl.needsClientAuth", "false");
            if (commandLineUtil.contains("-ssl_bind")) {
                properties.setProperty("server.ssl.bind", commandLineUtil.get("-ssl_bind"));
            } else {
                properties.setProperty("server.ssl.bind", "0.0.0.0");
            }
            if (commandLineUtil.contains("-ssl_port")) {
                properties.setProperty("server.ssl.port", commandLineUtil.get("-ssl_port"));
            }
            if (commandLineUtil.contains("-ssl_jks")) {
                properties.setProperty("server.ssl.jksPath", commandLineUtil.get("-ssl_jks"));
            }
            if (commandLineUtil.contains("-ssl_ks_pwd")) {
                properties.setProperty("server.ssl.keyStorePassword", commandLineUtil.get("-ssl_ks_pwd"));
            }
            if (commandLineUtil.contains("-ssl_km_pwd")) {
                properties.setProperty("server.ssl.keyManagerPassword", commandLineUtil.get("-ssl_km_pwd"));
            }
        }
  /*
        server.ssl.enable=true
        server.ssl.bind=0.0.0.0
        server.ssl.port=4993
        server.ssl.jksPath=test.jks
        server.ssl.keyStorePassword=123456
        server.ssl.keyManagerPassword=123456

        #这个配置可以忽略
        server.ssl.needsClientAuth=false
        */

        logger.info("command line properties: {}", properties.entrySet());
        Config.getInstance().loadProperties(properties);
        System.setProperty("artifact.version", readVersion());
        System.setProperty("jdk.tls.rejectClientInitiatedRenegotiation", "true");

        new Thread(new MetricsPersistenceWrapper(), "MetricsPersistence").start();

        ContainerHelper.start(Arrays.asList(new ProxyServerContainer(), new WebConfigContainer()));
    }


    private static String readVersion() {
        URL resource = ProxyServerContainer.class.getClassLoader().getResource("META-INF/MANIFEST.MF");
        try {
            Manifest manifest = new Manifest(resource.openStream());
            for (Map.Entry<Object, Object> entry : manifest.getMainAttributes().entrySet()) {
                if (entry.getKey().equals("Implementation-Version")) {
                    return entry.getValue().toString();
                }
            }
        } catch (Exception e) {
            logger.info("Can't read artifact version {}", e.toString());
        }
        return "";
    }

    @Override
    public void start() {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(serverBossGroup, serverWorkerGroup).channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<SocketChannel>() {

            @Override
            public void initChannel(SocketChannel ch) {
                ch.pipeline().addLast(new ProxyMessageDecoder(MAX_FRAME_LENGTH, LENGTH_FIELD_OFFSET, LENGTH_FIELD_LENGTH, LENGTH_ADJUSTMENT, INITIAL_BYTES_TO_STRIP));
                ch.pipeline().addLast(new ProxyMessageEncoder());
                ch.pipeline().addLast(new IdleCheckHandler(IdleCheckHandler.READ_IDLE_TIME, IdleCheckHandler.WRITE_IDLE_TIME, 0));
                ch.pipeline().addLast(new ServerChannelHandler());
            }
        });

        try {
            bootstrap.bind(ProxyConfig.getInstance().getServerBind(), ProxyConfig.getInstance().getServerPort()).get();
            logger.info("server start on {}:{} ", ProxyConfig.getInstance().getServerBind(), ProxyConfig.getInstance().getServerPort());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        if (Config.getInstance().getBooleanValue("server.ssl.enable", false)) {
            String host = Config.getInstance().getStringValue("server.ssl.bind", "0.0.0.0");
            int port = Config.getInstance().getIntValue("server.ssl.port");
            initializeSSLTCPTransport(host, port, new SslContextCreator().initSSLContext());
        }

        startUserPort();

    }

    private void initializeSSLTCPTransport(String host, int port, final SSLContext sslContext) {
        ServerBootstrap b = new ServerBootstrap();
        b.group(serverBossGroup, serverWorkerGroup).channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<SocketChannel>() {

            @Override
            public void initChannel(SocketChannel ch) {
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

    private void startUserPort() {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(serverBossGroup, serverWorkerGroup).channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<SocketChannel>() {

            @Override
            public void initChannel(SocketChannel ch) {
                ch.pipeline().addFirst(new BytesMetricsHandler());
                ch.pipeline().addLast(new UserChannelHandler());
            }
        });

        List<Integer> ports = ProxyConfig.getInstance().getUserPorts();
        for (int port : ports) {
            try {
                bootstrap.bind(port).get();
                logger.info("bind user port {}", port);
            } catch (Exception ex) {

                // BindException表示该端口已经绑定过
                if (!(ex.getCause() instanceof BindException)) {
                    throw new RuntimeException(ex);
                }
            }
        }

    }

}
