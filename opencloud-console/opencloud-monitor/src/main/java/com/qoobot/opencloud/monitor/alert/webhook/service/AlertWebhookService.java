package com.qoobot.opencloud.monitor.alert.webhook.service;

import com.qoobot.opencloud.monitor.alert.webhook.domain.dto.AlertManagerPayloadDTO;

/**
 * AlertManager Webhook 服务接口
 */
public interface AlertWebhookService {

    /**
     * 处理 AlertManager Webhook
     */
    void handleWebhook(AlertManagerPayloadDTO payload, String webhookSecret);
}
