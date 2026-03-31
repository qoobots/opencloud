package com.qoobot.opencloud.monitor.alert.webhook.controller;

import com.qoobot.opencloud.common.domain.R;
import com.qoobot.opencloud.monitor.alert.webhook.domain.dto.AlertManagerPayloadDTO;
import com.qoobot.opencloud.monitor.alert.webhook.service.AlertWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AlertManager Webhook 控制器
 * 注意：此接口在白名单中，无需 Token 认证
 */
@Slf4j
@RestController
@RequestMapping("/api/monitor/alerts")
@RequiredArgsConstructor
public class AlertWebhookController {

    private final AlertWebhookService alertWebhookService;

    /**
     * 接收 AlertManager Webhook
     */
    @PostMapping("/webhook")
    public R<Void> webhook(
            @RequestBody AlertManagerPayloadDTO payload,
            @RequestHeader(value = "X-Prometheus-Token", required = false) String webhookSecret) {

        log.info("收到 AlertManager Webhook，状态：{}，告警数：{}",
                payload.getStatus(),
                payload.getAlerts() != null ? payload.getAlerts().size() : 0);

        alertWebhookService.handleWebhook(payload, webhookSecret);

        return R.ok();
    }
}
