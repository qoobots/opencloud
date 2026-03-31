package com.qoobot.opencloud.monitor.alert.rule.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 告警规则状态更新 DTO
 */
@Data
public class AlertRuleStatusDTO {

    @NotBlank(message = "状态不能为空")
    @Pattern(regexp = "^(ENABLED|DISABLED)$", message = "状态必须是 ENABLED 或 DISABLED")
    private String status;
}
