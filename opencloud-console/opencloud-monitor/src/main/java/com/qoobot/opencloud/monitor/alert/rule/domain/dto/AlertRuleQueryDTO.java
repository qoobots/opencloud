package com.qoobot.opencloud.monitor.alert.rule.domain.dto;

import com.qoobot.opencloud.common.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 告警规则查询 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AlertRuleQueryDTO extends PageQuery {

    private String ruleName;

    private String severity;

    private String status;
}
