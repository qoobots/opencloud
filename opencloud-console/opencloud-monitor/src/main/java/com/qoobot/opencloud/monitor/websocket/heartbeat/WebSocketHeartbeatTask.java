package com.qoobot.opencloud.monitor.websocket.heartbeat;

import com.qoobot.opencloud.monitor.websocket.registry.WebSocketSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * WebSocket 心跳保活任务
 * 每 30s 向所有在线客户端发送 PING
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketHeartbeatTask {

    private final WebSocketSessionRegistry sessionRegistry;

    @Scheduled(fixedDelay = 30_000)
    public void sendPing() {
        int activeCount = sessionRegistry.getActiveCount();
        if (activeCount > 0) {
            sessionRegistry.broadcast("{\"type\":\"PING\"}");
            log.debug("WebSocket 心跳已发送，当前在线连接数: {}", activeCount);
        }
    }
}
