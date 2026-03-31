package com.qoobot.opencloud.monitor.notify.dispatch.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qoobot.opencloud.monitor.alert.record.domain.entity.MonitorAlertRecord;
import com.qoobot.opencloud.monitor.notify.channel.domain.entity.MonitorNotifyChannel;
import com.qoobot.opencloud.monitor.notify.channel.mapper.NotifyChannelMapper;
import com.qoobot.opencloud.monitor.notify.dispatch.NotifyDispatchService;
import com.qoobot.opencloud.monitor.notify.log.domain.entity.MonitorNotifyLog;
import com.qoobot.opencloud.monitor.notify.log.mapper.NotifyLogMapper;
import com.qoobot.opencloud.monitor.notify.sender.NotifyContext;
import com.qoobot.opencloud.monitor.notify.sender.NotifySender;
import com.qoobot.opencloud.monitor.util.AesEncryptUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 通知分发服务实现
 * <p>
 * 核心逻辑：
 * <ol>
 *   <li>解析 channelIdsJson，逐个加载通知渠道配置</li>
 *   <li>解密敏感字段，构建 {@link NotifyContext}</li>
 *   <li>调用对应 {@link NotifySender#send(NotifyContext)} 发送通知</li>
 *   <li>写入 {@link MonitorNotifyLog}，失败最多重试 3 次（指数退避）</li>
 * </ol>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyDispatchServiceImpl implements NotifyDispatchService {

    private static final int MAX_RETRY = 3;

    /** 敏感字段名称集合（与 NotifyChannelServiceImpl 保持一致） */
    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "smtpPass", "secret", "authorization", "token", "password");

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final NotifyChannelMapper notifyChannelMapper;
    private final NotifyLogMapper notifyLogMapper;
    private final List<NotifySender> notifySenders;
    private final ObjectMapper objectMapper;

    @Override
    @Async
    public void sendAsync(MonitorAlertRecord record, String channelIdsJson) {
        List<Long> channelIds = parseChannelIds(channelIdsJson);
        if (channelIds.isEmpty()) {
            log.debug("告警记录 {} 未绑定通知渠道，跳过通知", record.getId());
            return;
        }

        for (Long channelId : channelIds) {
            MonitorNotifyChannel channel = notifyChannelMapper.selectById(channelId);
            if (channel == null || channel.getDeleted() == 1) {
                log.warn("通知渠道不存在或已删除: channelId={}", channelId);
                continue;
            }

            NotifyContext ctx = buildContext(record, channel, false);
            sendWithRetry(record.getId(), channel, ctx);
        }
    }

    @Override
    @Async
    public void sendResolvedAsync(MonitorAlertRecord record, String channelIdsJson) {
        List<Long> channelIds = parseChannelIds(channelIdsJson);
        if (channelIds.isEmpty()) {
            return;
        }

        for (Long channelId : channelIds) {
            MonitorNotifyChannel channel = notifyChannelMapper.selectById(channelId);
            if (channel == null || channel.getDeleted() == 1) {
                log.warn("通知渠道不存在或已删除: channelId={}", channelId);
                continue;
            }

            NotifyContext ctx = buildContext(record, channel, true);
            sendWithRetry(record.getId(), channel, ctx);
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 发送通知，失败时最多重试 {@link #MAX_RETRY} 次（指数退避）
     */
    private void sendWithRetry(Long alertRecordId, MonitorNotifyChannel channel, NotifyContext ctx) {
        NotifySender sender = notifySenders.stream()
                .filter(s -> s.supportType().equalsIgnoreCase(channel.getChannelType()))
                .findFirst()
                .orElse(null);

        if (sender == null) {
            log.error("不支持的通知渠道类型: {}", channel.getChannelType());
            writeLog(alertRecordId, channel, "FAILED", "不支持的渠道类型: " + channel.getChannelType(), 0);
            return;
        }

        int attempt = 0;
        Exception lastException = null;

        while (attempt < MAX_RETRY) {
            try {
                sender.send(ctx);
                writeLog(alertRecordId, channel, "SUCCESS", null, attempt);
                log.info("通知发送成功: alertRecordId={}, channelId={}, channelType={}, attempt={}",
                        alertRecordId, channel.getId(), channel.getChannelType(), attempt + 1);
                return;
            } catch (Exception e) {
                lastException = e;
                attempt++;
                log.warn("通知发送失败（第 {}/{} 次）: alertRecordId={}, channelId={}, error={}",
                        attempt, MAX_RETRY, alertRecordId, channel.getId(), e.getMessage());

                if (attempt < MAX_RETRY) {
                    // 指数退避：1s, 2s, 4s
                    try {
                        Thread.sleep(1000L * (1L << (attempt - 1)));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        // 所有重试均失败
        String errorMsg = lastException != null ? lastException.getMessage() : "未知错误";
        writeLog(alertRecordId, channel, "FAILED", errorMsg, attempt);
        log.error("通知发送最终失败: alertRecordId={}, channelId={}, retries={}, error={}",
                alertRecordId, channel.getId(), attempt, errorMsg);
    }

    /**
     * 写入通知日志
     */
    private void writeLog(Long alertRecordId, MonitorNotifyChannel channel,
                          String status, String errorMsg, int retryCount) {
        MonitorNotifyLog log = new MonitorNotifyLog();
        log.setTenantId(channel.getTenantId());
        log.setAlertRecordId(alertRecordId);
        log.setChannelId(channel.getId());
        log.setChannelName(channel.getChannelName());
        log.setChannelType(channel.getChannelType());
        log.setIsTest(false);
        log.setStatus(status);
        log.setErrorMsg(errorMsg);
        log.setRetryCount(retryCount);
        log.setSentAt(LocalDateTime.now());
        notifyLogMapper.insert(log);
    }

    /**
     * 构建通知上下文（解密敏感字段）
     */
    private NotifyContext buildContext(MonitorAlertRecord record,
                                        MonitorNotifyChannel channel,
                                        boolean resolved) {
        Map<String, Object> decryptedConfig = decryptConfig(channel.getConfig());

        return NotifyContext.builder()
                .channel(channel)
                .decryptedConfig(decryptedConfig)
                .isTest(false)
                .alertRecordId(record.getId())
                .alertName(record.getAlertName())
                .severity(record.getSeverity())
                .instance(record.getInstance())
                .summary(record.getSummary())
                .firedAt(record.getFiredAt() != null
                        ? record.getFiredAt().format(FORMATTER) : "")
                .resolved(resolved)
                .resolvedAt(record.getResolvedAt() != null
                        ? record.getResolvedAt().format(FORMATTER) : "")
                .build();
    }

    /**
     * 解密配置中的敏感字段
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> decryptConfig(String configJson) {
        if (!StringUtils.hasText(configJson)) {
            return new HashMap<>();
        }
        try {
            Map<String, Object> configMap = objectMapper.readValue(configJson,
                    new TypeReference<Map<String, Object>>() {});
            for (String field : SENSITIVE_FIELDS) {
                if (configMap.containsKey(field) && configMap.get(field) != null) {
                    String encrypted = configMap.get(field).toString();
                    if (StringUtils.hasText(encrypted)) {
                        try {
                            configMap.put(field, AesEncryptUtil.decrypt(encrypted));
                        } catch (Exception e) {
                            log.warn("解密字段 {} 失败，可能未加密", field);
                        }
                    }
                }
            }
            return configMap;
        } catch (JsonProcessingException e) {
            log.error("反序列化通知渠道 config 失败", e);
            return new HashMap<>();
        }
    }

    /**
     * 解析 JSON 数组字符串为 Long 列表
     */
    private List<Long> parseChannelIds(String channelIdsJson) {
        if (!StringUtils.hasText(channelIdsJson) || "[]".equals(channelIdsJson.trim())) {
            return List.of();
        }
        try {
            return objectMapper.readValue(channelIdsJson, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.warn("解析 notifyChannelIds 失败: {}", channelIdsJson);
            return List.of();
        }
    }
}
