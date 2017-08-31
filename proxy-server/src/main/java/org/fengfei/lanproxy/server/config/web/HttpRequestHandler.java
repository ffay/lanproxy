package org.fengfei.lanproxy.server.config.web;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.URI;
import java.nio.charset.Charset;

import org.fengfei.lanproxy.common.JsonUtil;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpHeaders.Names;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedNioFile;

public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final String PAGE_FOLDER = System.getProperty("app.home", System.getProperty("user.dir"))
            + "/webpages";

    private static final String SERVER_VS = "LPS-0.1";

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {

        // GET返回页面；POST请求接口
        if (request.getMethod() != HttpMethod.POST) {
            outputPages(ctx, request);
            return;
        }

        ResponseInfo responseInfo = ApiRoute.run(request);

        // 错误码规则：除100取整为http状态码
        outputContent(ctx, request, responseInfo.getCode() / 100, JsonUtil.object2json(responseInfo),
                "Application/json;charset=utf-8");
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
        uriPath = uriPath.equals("/") ? "/index.html" : uriPath;
        String path = PAGE_FOLDER + uriPath;
        File rfile = new File(path);
        if (rfile.isDirectory()) {
            path = path + "/index.html";
            rfile = new File(path);
        }

        if (!rfile.exists()) {
            status = HttpResponseStatus.NOT_FOUND;
            outputContent(ctx, request, status.code(), status.toString(), "text/html");
            return;
        }

        if (HttpHeaders.is100ContinueExpected(request)) {
            send100Continue(ctx);
        }

        String mimeType = MimeType.getMimeType(MimeType.parseSuffix(path));
        long length = 0;
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(rfile, "r");
            length = raf.length();
        } finally {
            if (length < 0 && raf != null) {
                raf.close();
            }
        }

        HttpResponse response = new DefaultHttpResponse(request.getProtocolVersion(), status);
        response.headers().set(HttpHeaders.Names.CONTENT_TYPE, mimeType);
        boolean keepAlive = HttpHeaders.isKeepAlive(request);
        if (keepAlive) {
            response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, length);
            response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        }

        response.headers().set(Names.SERVER, SERVER_VS);
        ctx.write(response);

        if (ctx.pipeline().get(SslHandler.class) == null) {
            ctx.write(new DefaultFileRegion(raf.getChannel(), 0, length));
        } else {
            ctx.write(new ChunkedNioFile(raf.getChannel()));
        }

        ChannelFuture future = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        if (!keepAlive) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }

    private static void send100Continue(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE);
        ctx.writeAndFlush(response);
    }

}
