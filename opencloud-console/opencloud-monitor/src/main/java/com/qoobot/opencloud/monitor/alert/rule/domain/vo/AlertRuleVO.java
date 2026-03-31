package com.qoobot.opencloud.monitor.alert.rule.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 告警规则 VO
 */
@Data
public class AlertRuleVO {

    private Long id;

    private String tenantId;

    private String ruleName;

    private String description;

    private String promqlExpr;

    private String severity;

    private String duration;

    private List<Long> notifyChannelIds;

    private Boolean notifyOnResolve;

    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
