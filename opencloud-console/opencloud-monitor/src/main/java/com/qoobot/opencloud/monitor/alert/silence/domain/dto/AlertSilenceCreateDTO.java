package com.qoobot.opencloud.monitor.alert.silence.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 告警静默创建/更新 DTO
 */
@Data
public class AlertSilenceCreateDTO {

    @NotBlank(message = "静默名称不能为空")
    private String silenceName;

    private String description;

    /**
     * 标签匹配规则，JSON 数组格式
     * 示例：[{"key":"alertname","value":"NodeDown"},{"key":"severity","value":"CRITICAL"}]
     */
    @NotBlank(message = "匹配规则不能为空")
    private String matchLabels;

    @NotNull(message = "静默开始时间不能为空")
    private LocalDateTime startAt;

    @NotNull(message = "静默结束时间不能为空")
    private LocalDateTime endAt;
}
