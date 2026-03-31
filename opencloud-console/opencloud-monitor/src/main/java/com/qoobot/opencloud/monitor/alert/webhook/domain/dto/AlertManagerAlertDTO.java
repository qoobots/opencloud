package com.qoobot.opencloud.monitor.alert.webhook.domain.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * AlertManager Alert DTO
 */
@Data
public class AlertManagerAlertDTO {

    private String status;

    private Map<String, String> labels;

    private Map<String, String> annotations;

    private LocalDateTime startsAt;

    private LocalDateTime endsAt;

    private String generatorURL;

    private String fingerprint;
}
