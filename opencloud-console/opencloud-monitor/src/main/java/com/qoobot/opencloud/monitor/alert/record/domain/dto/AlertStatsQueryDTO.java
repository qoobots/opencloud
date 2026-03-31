package com.qoobot.opencloud.monitor.alert.record.domain.dto;

import lombok.Data;

/**
 * 告警统计查询 DTO
 */
@Data
public class AlertStatsQueryDTO {

    /**
     * 统计周期：today / 7d / 30d
     */
    private String period = "today";
}
