package com.qoobot.opencloud.monitor.alert.silence.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 告警静默实体
 */
@Data
@TableName("monitor_alert_silence")
public class MonitorAlertSilence {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String tenantId;

    private String silenceName;

    private String description;

    /** 标签匹配规则（JSON 数组，如 [{"key":"alertname","value":"NodeDown"}]） */
    private String matchLabels;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    /** ACTIVE / EXPIRED */
    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String createdBy;

    private String updatedBy;
}
