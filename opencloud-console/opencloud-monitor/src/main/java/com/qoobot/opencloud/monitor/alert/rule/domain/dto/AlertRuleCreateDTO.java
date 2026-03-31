package com.qoobot.opencloud.monitor.alert.rule.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 告警规则创建 DTO
 */
@Data
public class AlertRuleCreateDTO {

    @NotBlank(message = "规则名称不能为空")
    @Size(max = 100, message = "规则名称长度不能超过100")
    private String ruleName;

    @Size(max = 500, message = "规则描述长度不能超过500")
    private String description;

    @NotBlank(message = "PromQL表达式不能为空")
    private String promqlExpr;

    @NotBlank(message = "严重级别不能为空")
    @Pattern(regexp = "^(INFO|WARNING|CRITICAL)$", message = "严重级别必须是 INFO、WARNING 或 CRITICAL")
    private String severity;

    @NotBlank(message = "持续时间不能为空")
    @Pattern(regexp = "^[1-9][0-9]?m$", message = "持续时间格式错误，如 5m")
    private String duration;

    private List<Long> notifyChannelIds;

    @NotNull(message = "notifyOnResolve不能为空")
    private Boolean notifyOnResolve = false;
}
