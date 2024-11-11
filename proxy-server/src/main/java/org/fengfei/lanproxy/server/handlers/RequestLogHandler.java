package org.fengfei.lanproxy.server.handlers;

import io.netty.channel.Channel;
import org.fengfei.lanproxy.server.requestlogs.RequestLog;
import org.fengfei.lanproxy.server.requestlogs.RequestLogCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class RequestLogHandler {
    private static final Logger logger = LoggerFactory.getLogger(RequestLogHandler.class);

    // 使用队列异步处理日志
    private final BlockingQueue<RequestLog> logQueue = new LinkedBlockingQueue<>();

    public void logRequest(Channel channel, String requestInfo) {
        RequestLog log = new RequestLog();
        InetSocketAddress address = (InetSocketAddress) channel.remoteAddress();
        log.setIp(address.getAddress().getHostAddress());
        log.setPort(address.getPort());
        log.setTime(new Date());
        log.setRequestInfo(requestInfo);

        // 异步写入日志队列
        logQueue.offer(log);
    }

    // 日志处理线程
    public class LogProcessor implements Runnable {
        public void run() {
            while (true) {
                try {
                    RequestLog log = logQueue.take();
                    // 写入日志文件或数据库
                    logger.info("Request from {}:{}", log.getIp(), log.getPort());

                    // 修改日志处理逻辑，保存最近的日志
                    saveLog(log);
                } catch (Exception e) {
                    logger.error("Error processing log", e);
                }
            }
        }
    }

    // 修改日志处理逻辑，保存最近的日志
    private void saveLog(RequestLog log) {
        RequestLogCollector.saveLog(log);
    }


    // 添加日志VO类用于页面展示
    public static class RequestLogVO {
        private String ip;
        private int port;
        private String time;
        private String requestInfo;

        public RequestLogVO(RequestLog log) {
            this.ip = log.getIp();
            this.port = log.getPort();
            this.time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(log.getTime());
            this.requestInfo = log.getRequestInfo();
        }

        // getter方法
        public String getIp() {
            return ip;
        }

        public int getPort() {
            return port;
        }

        public String getTime() {
            return time;
        }

        public String getRequestInfo() {
            return requestInfo;
        }
    }
}
