package com.qoobot.opencloud.monitor.alert.silence.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 告警静默 VO
 */
@Data
public class AlertSilenceVO {

    private Long id;

    private String silenceName;

    private String description;

    /** 标签匹配规则（JSON 数组字符串） */
    private String matchLabels;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    /** ACTIVE / EXPIRED */
    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String createdBy;
}
