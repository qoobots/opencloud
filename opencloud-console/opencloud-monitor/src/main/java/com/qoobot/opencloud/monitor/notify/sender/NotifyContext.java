package com.qoobot.opencloud.monitor.notify.sender;

import com.qoobot.opencloud.monitor.notify.channel.domain.entity.MonitorNotifyChannel;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 通知发送上下文
 */
@Data
@Builder
public class NotifyContext {

    /** 通知渠道（原始实体） */
    private MonitorNotifyChannel channel;

    /** 解密后的配置 Map */
    private Map<String, Object> decryptedConfig;

    /** 是否为测试发送 */
    private boolean isTest;

    /** 测试消息内容 */
    private String testMessage;

    /** 告警记录 ID */
    private Long alertRecordId;

    /** 告警名称 */
    private String alertName;

    /** 严重级别 */
    private String severity;

    /** 实例地址 */
    private String instance;

    /** 告警摘要 */
    private String summary;

    /** 告警触发时间 */
    private String firedAt;

    /** 是否为恢复通知 */
    private boolean resolved;

    /** 告警解除时间 */
    private String resolvedAt;

    /**
     * 获取指定类型的配置值
     */
    public String getConfigStr(String key) {
        if (decryptedConfig == null) return null;
        Object val = decryptedConfig.get(key);
        return val != null ? val.toString() : null;
    }

    public Integer getConfigInt(String key, int defaultVal) {
        if (decryptedConfig == null) return defaultVal;
        Object val = decryptedConfig.get(key);
        if (val == null) return defaultVal;
        try {
            return Integer.parseInt(val.toString());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    public Boolean getConfigBool(String key, boolean defaultVal) {
        if (decryptedConfig == null) return defaultVal;
        Object val = decryptedConfig.get(key);
        if (val == null) return defaultVal;
        return Boolean.parseBoolean(val.toString());
    }
}
