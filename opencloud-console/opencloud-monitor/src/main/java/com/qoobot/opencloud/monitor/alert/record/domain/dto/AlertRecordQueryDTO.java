package com.qoobot.opencloud.monitor.alert.record.domain.dto;

import com.qoobot.opencloud.common.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 告警记录查询 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AlertRecordQueryDTO extends PageQuery {

    private String alertName;

    private String severity;

    private String status;

    private String instance;

    private Long ruleId;

    private LocalDateTime beginTime;

    private LocalDateTime endTime;
}
