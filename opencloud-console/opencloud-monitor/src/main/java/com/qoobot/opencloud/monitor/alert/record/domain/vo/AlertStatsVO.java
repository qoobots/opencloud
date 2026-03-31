package com.qoobot.opencloud.monitor.alert.record.domain.vo;

import lombok.Data;

/**
 * 告警统计 VO
 */
@Data
public class AlertStatsVO {

    /**
     * 总触发次数
     */
    private Long total;

    /**
     * 当前未解决数
     */
    private Long firing;

    /**
     * 已确认数
     */
    private Long acknowledged;

    /**
     * 已解决数
     */
    private Long resolved;

    /**
     * CRITICAL 级别数
     */
    private Long criticalCount;

    /**
     * WARNING 级别数
     */
    private Long warningCount;

    /**
     * INFO 级别数
     */
    private Long infoCount;

    /**
     * 平均持续时间（秒）
     */
    private Double avgDurationSeconds;
}
