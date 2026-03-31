package com.qoobot.opencloud.monitor.alert.webhook.domain.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * AlertManager Webhook Payload DTO
 */
@Data
public class AlertManagerPayloadDTO {

    private String version;

    private String groupKey;

    private String status;

    private String receiver;

    private List<AlertManagerAlertDTO> alerts;

    private Map<String, Object> groupLabels;

    private Map<String, Object> commonLabels;

    private Map<String, Object> commonAnnotations;

    private String externalURL;
}
