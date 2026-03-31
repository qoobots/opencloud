package com.qoobot.opencloud.monitor.websocket.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qoobot.opencloud.monitor.alert.record.domain.entity.MonitorAlertRecord;
import com.qoobot.opencloud.monitor.websocket.registry.WebSocketSessionRegistry;
import com.qoobot.opencloud.monitor.websocket.service.AlertPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 告警推送服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertPushServiceImpl implements AlertPushService {

    private final WebSocketSessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;

    @Override
    public void broadcastAlertFiring(MonitorAlertRecord record) {
        try {
            Map<String, Object> msg = new HashMap<>();
            msg.put("type", "ALERT_FIRING");
            msg.put("recordId", record.getId());
            msg.put("alertName", record.getAlertName());
            msg.put("severity", record.getSeverity());
            msg.put("instance", record.getInstance());
            msg.put("summary", record.getSummary());
            msg.put("firedAt", record.getFiredAt() != null ? record.getFiredAt().toString() : null);

            String json = objectMapper.writeValueAsString(msg);
            sessionRegistry.broadcast(json);

            log.debug("告警触发事件已推送: alertName={}, severity={}", record.getAlertName(), record.getSeverity());
        } catch (Exception e) {
            log.error("广播告警触发事件失败: {}", e.getMessage());
        }
    }

    @Override
    public void broadcastAlertResolved(MonitorAlertRecord record) {
        try {
            Map<String, Object> msg = new HashMap<>();
            msg.put("type", "ALERT_RESOLVED");
            msg.put("recordId", record.getId());
            msg.put("alertName", record.getAlertName());
            msg.put("resolvedAt", record.getResolvedAt() != null ? record.getResolvedAt().toString() : null);

            String json = objectMapper.writeValueAsString(msg);
            sessionRegistry.broadcast(json);

            log.debug("告警解除事件已推送: alertName={}", record.getAlertName());
        } catch (Exception e) {
            log.error("广播告警解除事件失败: {}", e.getMessage());
        }
    }

    @Override
    public void broadcast(String message) {
        sessionRegistry.broadcast(message);
    }
}
