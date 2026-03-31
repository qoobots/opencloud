package com.qoobot.opencloud.monitor.alert.silence.domain.dto;

import lombok.Data;

/**
 * 告警静默分页查询 DTO
 */
@Data
public class AlertSilenceQueryDTO {

    private String silenceName;

    /** ACTIVE / EXPIRED */
    private String status;

    private Integer pageNum = 1;

    private Integer pageSize = 20;
}
