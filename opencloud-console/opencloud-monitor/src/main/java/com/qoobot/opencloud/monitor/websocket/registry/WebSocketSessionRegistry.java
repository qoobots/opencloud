package com.qoobot.opencloud.monitor.websocket.registry;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket Session 注册表
 * 维护 userId -> WebSocketSession 的映射
 */
@Slf4j
@Component
public class WebSocketSessionRegistry {

    /** userId -> WebSocketSession */
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    /** sessionId -> userId（反向索引，用于断连时找 userId） */
    private final ConcurrentHashMap<String, String> sessionIdToUserId = new ConcurrentHashMap<>();

    /**
     * 注册 Session（同一用户新连接替换旧连接）
     */
    public void register(String userId, WebSocketSession newSession) {
        WebSocketSession old = sessions.put(userId, newSession);
        if (old != null && old.isOpen()) {
            try {
                old.close();
                log.info("关闭用户 {} 的旧 WebSocket 连接", userId);
            } catch (Exception ignored) {}
        }
        sessionIdToUserId.put(newSession.getId(), userId);
        log.info("WebSocket 注册成功: userId={}, sessionId={}", userId, newSession.getId());
    }

    /**
     * 按 Session 取消注册
     */
    public void unregisterBySession(WebSocketSession session) {
        String userId = sessionIdToUserId.remove(session.getId());
        if (userId != null) {
            sessions.remove(userId, session);
            log.info("WebSocket 断连: userId={}, sessionId={}", userId, session.getId());
        }
    }

    /**
     * 广播消息至所有活跃 Session
     */
    public void broadcast(String message) {
        sessions.values().forEach(session -> {
            if (session.isOpen()) {
                try {
                    synchronized (session) {
                        session.sendMessage(new TextMessage(message));
                    }
                } catch (Exception e) {
                    log.warn("WebSocket 广播失败，sessionId={}: {}", session.getId(), e.getMessage());
                    unregisterBySession(session);
                }
            }
        });
    }

    /**
     * 向指定用户发送消息
     */
    public void sendToUser(String userId, String message) {
        WebSocketSession session = sessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                synchronized (session) {
                    session.sendMessage(new TextMessage(message));
                }
            } catch (Exception e) {
                log.warn("WebSocket 定向发送失败，userId={}: {}", userId, e.getMessage());
                unregisterBySession(session);
            }
        }
    }

    /**
     * 获取当前活跃连接数
     */
    public int getActiveCount() {
        return (int) sessions.values().stream()
                .filter(WebSocketSession::isOpen)
                .count();
    }
}
