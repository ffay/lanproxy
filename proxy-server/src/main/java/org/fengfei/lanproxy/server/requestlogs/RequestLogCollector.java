package org.fengfei.lanproxy.server.requestlogs;

import org.fengfei.lanproxy.server.handlers.RequestLogHandler;

import java.util.ArrayList;
import java.util.List;

public class RequestLogCollector {
    private static final List<RequestLog> recentLogs = new ArrayList<>();
    // 存储最近的日志记录
    private static final int MAX_LOG_SIZE = 1000;

    // 添加获取日志的方法
    public static List<RequestLogHandler.RequestLogVO> getRecentLogs() {
        List<RequestLogHandler.RequestLogVO> logs = new ArrayList<>();
        synchronized (recentLogs) {
            for (RequestLog log : recentLogs) {
                logs.add(new RequestLogHandler.RequestLogVO(log));
            }
        }
        return logs;
    }

    // 修改日志处理逻辑，保存最近的日志
    public static void saveLog(RequestLog log) {
        synchronized (recentLogs) {
            if (recentLogs.size() >= MAX_LOG_SIZE) {
                recentLogs.remove(0);
            }
            recentLogs.add(log);
        }
    }
}
