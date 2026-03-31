package com.qoobot.opencloud.monitor.websocket.interceptor;

import com.qoobot.opencloud.monitor.util.MonitorJwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 握手拦截器（校验 Token）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final MonitorJwtUtil jwtUtil;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {

        if (!(request instanceof ServletServerHttpRequest)) {
            return false;
        }

        ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
        String token = servletRequest.getServletRequest().getParameter("token");

        if (token == null || token.isEmpty()) {
            log.warn("WebSocket 握手拒绝：缺少 Token");
            return false;
        }

        try {
            if (!jwtUtil.validateToken(token)) {
                log.warn("WebSocket 握手拒绝：Token 无效");
                return false;
            }

            String userId = jwtUtil.getUserId(token);
            String tenantId = jwtUtil.getTenantId(token);

            attributes.put("userId", userId);
            attributes.put("tenantId", tenantId);
            attributes.put("token", token);

            log.debug("WebSocket 握手认证成功: userId={}", userId);
            return true;

        } catch (Exception e) {
            log.warn("WebSocket 握手拒绝：Token 校验异常 - {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                WebSocketHandler wsHandler, Exception exception) {
        // 握手后无需处理
    }
}
