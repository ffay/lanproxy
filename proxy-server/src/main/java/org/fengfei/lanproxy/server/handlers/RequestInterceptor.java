package org.fengfei.lanproxy.server.handlers;

import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RequestInterceptor {
    
    private static final int MAX_REQUESTS_PER_SECOND = 100; // 每秒最大请求数
    private static final int BLACK_LIST_THRESHOLD = 1000; // 加入黑名单阈值
    private static final Logger logger = LoggerFactory.getLogger(RequestInterceptor.class);
    
    // 使用ConcurrentHashMap存储IP访问统计
    private final ConcurrentHashMap<String, AccessStats> statsMap = new ConcurrentHashMap<>();
    
    // IP黑名单
    private final Set<String> blackList = ConcurrentHashMap.newKeySet();
    
    public boolean interceptRequest(Channel channel) {
        InetSocketAddress address = (InetSocketAddress) channel.remoteAddress();
        String ip = address.getAddress().getHostAddress();
        
        // 检查是否在黑名单中
        if (blackList.contains(ip)) {
            logger.warn("Blocked request from blacklisted IP: {}", ip);
            return true;
        }
        
        // 获取或创建访问统计
        AccessStats stats = statsMap.computeIfAbsent(ip, k -> new AccessStats());
        
        // 检查频率限制
        if (stats.isExceedingLimit()) {
            stats.incrementBlockCount();
            
            // 超过阈值加入黑名单
            if (stats.getBlockCount() > BLACK_LIST_THRESHOLD) {
                blackList.add(ip);
                logger.warn("Added IP to blacklist: {}", ip);
            }
            
            return true;
        }
        
        // 记录请求
        stats.recordRequest();
        return false;
    }
    
    private static class AccessStats {
        private AtomicInteger requestCount = new AtomicInteger(0);
        private AtomicInteger blockCount = new AtomicInteger(0);
        private long lastResetTime = System.currentTimeMillis();
        
        public boolean isExceedingLimit() {
            resetIfNeeded();
            return requestCount.get() >= MAX_REQUESTS_PER_SECOND;
        }
        
        public void recordRequest() {
            resetIfNeeded();
            requestCount.incrementAndGet();
        }
        
        public void incrementBlockCount() {
            blockCount.incrementAndGet(); 
        }
        
        public int getBlockCount() {
            return blockCount.get();
        }
        
        private void resetIfNeeded() {
            long now = System.currentTimeMillis();
            if (now - lastResetTime > 1000) {
                requestCount.set(0);
                lastResetTime = now;
            }
        }
    }
}