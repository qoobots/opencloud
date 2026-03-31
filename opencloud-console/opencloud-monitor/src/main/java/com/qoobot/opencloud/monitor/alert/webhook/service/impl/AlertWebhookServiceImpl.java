package com.qoobot.opencloud.monitor.alert.webhook.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qoobot.opencloud.common.util.TenantContext;
import com.qoobot.opencloud.monitor.alert.record.domain.entity.MonitorAlertRecord;
import com.qoobot.opencloud.monitor.alert.record.mapper.AlertRecordMapper;
import com.qoobot.opencloud.monitor.alert.rule.domain.entity.MonitorAlertRule;
import com.qoobot.opencloud.monitor.alert.rule.mapper.AlertRuleMapper;
import com.qoobot.opencloud.monitor.alert.silence.service.AlertSilenceService;
import com.qoobot.opencloud.monitor.alert.webhook.domain.dto.AlertManagerAlertDTO;
import com.qoobot.opencloud.monitor.alert.webhook.domain.dto.AlertManagerPayloadDTO;
import com.qoobot.opencloud.monitor.alert.webhook.service.AlertWebhookService;
import com.qoobot.opencloud.monitor.notify.dispatch.NotifyDispatchService;
import com.qoobot.opencloud.monitor.websocket.service.AlertPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * AlertManager Webhook 服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertWebhookServiceImpl implements AlertWebhookService {

    @Value("${opencloud.monitor.webhook-secret:}")
    private String configuredSecret;

    private final AlertRecordMapper alertRecordMapper;
    private final AlertRuleMapper alertRuleMapper;
    private final AlertPushService alertPushService;
    private final AlertSilenceService alertSilenceService;
    private final NotifyDispatchService notifyDispatchService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleWebhook(AlertManagerPayloadDTO payload, String webhookSecret) {
        // 签名校验
        if (configuredSecret != null && !configuredSecret.isEmpty()) {
            if (!configuredSecret.equals(webhookSecret)) {
                log.error("Webhook 签名验证失败");
                // 不抛出异常，避免 AlertManager 重试
                return;
            }
        }

        if (payload.getAlerts() == null || payload.getAlerts().isEmpty()) {
            log.warn("Webhook payload 中无告警数据");
            return;
        }

        for (AlertManagerAlertDTO alert : payload.getAlerts()) {
            if ("firing".equals(alert.getStatus())) {
                handleFiring(alert);
            } else if ("resolved".equals(alert.getStatus())) {
                handleResolved(alert);
            }
        }
    }

    private void handleFiring(AlertManagerAlertDTO alert) {
        String alertName = alert.getLabels() != null ? alert.getLabels().get("alertname") : null;
        String instance = alert.getLabels() != null ? alert.getLabels().getOrDefault("instance", "") : "";

        if (alertName == null) {
            log.warn("告警缺少 alertname 标签");
            return;
        }

        // 幂等检查
        MonitorAlertRecord existing = alertRecordMapper.findByDedup(
                alertName, instance, alert.getStartsAt());
        if (existing != null) {
            log.debug("告警已存在，跳过写入：{} @ {}", alertName, instance);
            return;
        }

        // 匹配规则
        String tenantId = TenantContext.get();
        MonitorAlertRule rule = alertRuleMapper.findByAlertName(alertName, tenantId);

        String severity = rule != null ? rule.getSeverity()
                : alert.getLabels().getOrDefault("severity", "WARNING").toUpperCase();

        // 写入告警记录
        MonitorAlertRecord record = new MonitorAlertRecord();
        record.setTenantId(tenantId);
        record.setRuleId(rule != null ? rule.getId() : null);
        record.setRuleName(rule != null ? rule.getRuleName() : alertName);
        record.setAlertName(alertName);
        record.setSeverity(severity);
        record.setInstance(instance);
        record.setStatus("FIRING");
        record.setFiredAt(alert.getStartsAt());
        record.setNotified(false);

        // 设置 summary
        if (alert.getAnnotations() != null) {
            record.setSummary(alert.getAnnotations().getOrDefault("summary", ""));
        }

        // JSON 序列化 labels 和 annotations
        try {
            record.setLabels(objectMapper.writeValueAsString(alert.getLabels()));
            record.setAnnotations(objectMapper.writeValueAsString(alert.getAnnotations()));
        } catch (JsonProcessingException e) {
            log.error("序列化 labels/annotations 失败: {}", e.getMessage());
        }

        alertRecordMapper.insert(record);

        // WebSocket 推送
        alertPushService.broadcastAlertFiring(record);

        // 静默检查 + 异步通知
        if (rule != null && rule.getNotifyChannelIds() != null && !rule.getNotifyChannelIds().isEmpty()) {
            boolean silenced = alertSilenceService.isAlertSilenced(
                    alert.getLabels() != null ? alert.getLabels() : Map.of());
            if (silenced) {
                log.info("告警命中静默规则，跳过通知: alertName={}, instance={}", alertName, instance);
            } else {
                notifyDispatchService.sendAsync(record, rule.getNotifyChannelIds());
            }
        }
    }

    private void handleResolved(AlertManagerAlertDTO alert) {
        String alertName = alert.getLabels() != null ? alert.getLabels().get("alertname") : null;
        String instance = alert.getLabels() != null ? alert.getLabels().getOrDefault("instance", "") : "";

        if (alertName == null) {
            return;
        }

        // 查找对应的 FIRING 记录
        MonitorAlertRecord record = alertRecordMapper.findFiringByAlertAndInstance(alertName, instance);
        if (record == null) {
            log.warn("未找到对应 FIRING 记录，忽略 resolved：{} @ {}", alertName, instance);
            return;
        }

        record.setStatus("RESOLVED");
        record.setResolvedAt(alert.getEndsAt());
        alertRecordMapper.updateById(record);

        // WebSocket 推送
        alertPushService.broadcastAlertResolved(record);

        // 恢复通知
        if (record.getRuleId() != null) {
            MonitorAlertRule rule = alertRuleMapper.selectById(record.getRuleId());
            if (rule != null && Boolean.TRUE.equals(rule.getNotifyOnResolve())
                    && StringUtils.hasText(rule.getNotifyChannelIds())) {
                notifyDispatchService.sendResolvedAsync(record, rule.getNotifyChannelIds());
            }
        }
    }
}
