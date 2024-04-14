package cn.dxbtech.portbridge.server.config.web;

import cn.dxbtech.portbridge.commons.JsonUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.HttpHeaders.Names;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.nio.charset.Charset;

public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final String PAGE_FOLDER = System.getProperty("web.pages", "config-web-2");
    //    private static final String PAGE_FOLDER = System.getProperty("web.pages", String.join(File.separator, System.getProperty("user.dir"), "src", "main", "resources", "webpages"));
    private static final String SERVER_VS = "port-bridge";
    private static Logger logger = LoggerFactory.getLogger(HttpRequestHandler.class);

    private static void send100Continue(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE);
        ctx.writeAndFlush(response);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {

        //接收 request body
        String body = null;
        if (request.content() != null && request.content().readableBytes() > 0) {
            byte[] content = new byte[request.content().readableBytes()];
            request.content().getBytes(0, content);
            body = new String(content, "UTF-8");
        }

        //身份验证
        AuthConfig authConfig = AuthConfig.instance;
        String finalBody = body;
        new Thread(() -> {
            try {
                if (authConfig.logout(request.getUri())) {
                    outputContent(ctx, request, HttpResponseStatus.UNAUTHORIZED.code(), "logout success. <a href='/'>back</a>", MimeType.getMimeType("html"));
                }

                if (!authConfig.valid(request)) {
                    // 验证失败
                    notAuth(ctx, request, authConfig.errorInfo());
                    return;
                }


                // 如果url没有被api占用，那么返回页面
                if (!ApiRoute.isApi(request.getUri())) {
                    outputPages(ctx, request);
                    return;
                }

                ResponseInfo responseInfo = ApiRoute.run(request, finalBody);
                // 错误码规则：除100取整为http状态码
                outputContent(ctx, request, responseInfo.getStatus().code(), JsonUtil.object2json(responseInfo.getData()),
                        "Application/json;charset=utf-8");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "web-config-handler").start();
    }

    private void outputContent(ChannelHandlerContext ctx, FullHttpRequest request, int code, String content,
                               String mimeType) {

        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(code),
                Unpooled.wrappedBuffer(content.getBytes(Charset.forName("UTF-8"))));
        response.headers().set(Names.CONTENT_TYPE, mimeType);
        response.headers().set(Names.CONTENT_LENGTH, response.content().readableBytes());
        response.headers().set(Names.SERVER, SERVER_VS);
        ChannelFuture future = ctx.writeAndFlush(response);
        if (!HttpHeaders.isKeepAlive(request)) {
            future.addListener(ChannelFutureListener.CLOSE);
        }

    }

    private void notAuth(ChannelHandlerContext ctx, FullHttpRequest request, String errorInfo) {
        if (errorInfo == null) errorInfo = "";
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.UNAUTHORIZED,
                Unpooled.wrappedBuffer((HttpResponseStatus.FORBIDDEN.reasonPhrase() + ": " + errorInfo).getBytes(Charset.forName("UTF-8"))));
        response.headers().set(Names.CONTENT_TYPE, MimeType.getMimeType("html"));
        response.headers().set(Names.CONTENT_LENGTH, response.content().readableBytes());
        response.headers().set(Names.SERVER, SERVER_VS);
        response.headers().set(Names.WWW_AUTHENTICATE, String.format("Basic realm=\"%s\"", errorInfo));
        ChannelFuture future = ctx.writeAndFlush(response);
        if (!HttpHeaders.isKeepAlive(request)) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }


    private byte[] input2byte(InputStream inStream) throws IOException {
        ByteArrayOutputStream swapStream = new ByteArrayOutputStream();
        byte[] buff = new byte[100];
        int rc = 0;
        while ((rc = inStream.read(buff, 0, 100)) > 0) {
            swapStream.write(buff, 0, rc);
        }
        byte[] in2b = swapStream.toByteArray();
        return in2b;
    }

    private byte[] getBytesFromPath(String path) {
        InputStream inputStream = null;

        try {
            inputStream = System.getProperty("web.pages") != null ?
                    new FileInputStream(new File(path)) : getClass().getClassLoader().getResourceAsStream(path);

            return input2byte(inputStream);
        } catch (Exception e) {
            logger.warn("{}: e:{}", path, e.toString());
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception ignore) {

                }
            }
        }
    }

    /**
     * 输出静态资源数据
     *
     * @param ctx
     * @param request
     * @throws Exception
     */
    private void outputPages(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        HttpResponseStatus status = HttpResponseStatus.OK;
        URI uri = new URI(request.getUri());
        String uriPath = uri.getPath();
        uriPath = uriPath.equals("/") ? "/mappings.html" : uriPath; // 默认访问index.html
        String path = PAGE_FOLDER + uriPath;

        byte[] bytes = getBytesFromPath(path);

        if (bytes == null) {
            status = HttpResponseStatus.NOT_FOUND;
            outputContent(ctx, request, status.code(), status.toString(), MimeType.getMimeType("html"));
            return;
        }

        if (HttpHeaders.is100ContinueExpected(request)) {
            send100Continue(ctx);
        }

        String mimeType = MimeType.getMimeTypeByPath(path);
        long length = bytes.length;

        DefaultFullHttpResponse response = new DefaultFullHttpResponse(request.getProtocolVersion(), status);
        response.headers().set(Names.CONTENT_TYPE, mimeType);
        boolean keepAlive = HttpHeaders.isKeepAlive(request);
        if (keepAlive) {
            response.headers().set(Names.CONTENT_LENGTH, length);
            response.headers().set(Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        }

        response.headers().set(Names.SERVER, SERVER_VS);
        response.content().writeBytes(bytes);

        if (keepAlive) {
            ctx.writeAndFlush(response);
        } else {
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }
    }

    interface AuthConfig {
        AuthConfig instance = new AuthConfigImpl();

        boolean logout(String url);

        boolean valid(FullHttpRequest request);

        String errorInfo();
    }

}
