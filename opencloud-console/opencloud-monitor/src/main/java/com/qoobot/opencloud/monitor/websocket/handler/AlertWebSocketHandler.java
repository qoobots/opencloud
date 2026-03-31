package com.qoobot.opencloud.monitor.websocket.handler;

import com.qoobot.opencloud.monitor.websocket.registry.WebSocketSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * 告警 WebSocket Handler
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertWebSocketHandler extends TextWebSocketHandler {

    private final WebSocketSessionRegistry sessionRegistry;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 从 URI query 参数中提取 token 并校验
        String query = session.getUri() != null ? session.getUri().getQuery() : null;
        String userId = extractUserId(session, query);

        if (userId == null) {
            log.warn("WebSocket 连接拒绝：Token 无效，sessionId={}", session.getId());
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Token 无效或已过期"));
            return;
        }

        sessionRegistry.register(userId, session);

        // 发送握手确认消息
        session.sendMessage(new TextMessage(
                "{\"type\":\"CONNECTED\",\"message\":\"WebSocket 连接成功\",\"userId\":\"" + userId + "\"}"));

        log.info("WebSocket 连接建立: userId={}, sessionId={}, 当前在线={}",
                userId, session.getId(), sessionRegistry.getActiveCount());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 处理客户端发来的消息（目前只处理 PONG 心跳）
        String payload = message.getPayload();
        if (payload.contains("\"type\":\"PONG\"")) {
            // 收到心跳响应，不做处理
            log.debug("收到 PONG，sessionId={}", session.getId());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionRegistry.unregisterBySession(session);
        log.info("WebSocket 连接关闭: sessionId={}, status={}, 当前在线={}",
                session.getId(), status, sessionRegistry.getActiveCount());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("WebSocket 传输错误: sessionId={}, error={}", session.getId(), exception.getMessage());
        sessionRegistry.unregisterBySession(session);
    }

    /**
     * 从 Token 中提取 userId
     * URI 格式：ws://{host}/ws/alerts?token={accessToken}
     */
    private String extractUserId(WebSocketSession session, String query) {
        if (query == null || query.isEmpty()) {
            return null;
        }

        // 解析 query 参数
        String token = null;
        for (String param : query.split("&")) {
            String[] kv = param.split("=", 2);
            if (kv.length == 2 && "token".equals(kv[0])) {
                token = kv[1];
                break;
            }
        }

        if (token == null || token.isEmpty()) {
            return null;
        }

        // 从 session attributes 中取 userId（已由 HandshakeInterceptor 验证并放入）
        Object userId = session.getAttributes().get("userId");
        return userId != null ? userId.toString() : null;
    }
}
