package com.qoobot.opencloud.monitor.notify.sender.impl;

import com.qoobot.opencloud.monitor.notify.sender.NotifyContext;
import com.qoobot.opencloud.monitor.notify.sender.NotifySender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

/**
 * 自定义 Webhook 通知发送器（支持 Freemarker 模板）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookNotifySender implements NotifySender {

    private final WebClient.Builder webClientBuilder;

    @Override
    public String supportType() {
        return "WEBHOOK";
    }

    @Override
    public void send(NotifyContext ctx) throws Exception {
        String url = ctx.getConfigStr("url");
        String method = ctx.getConfigStr("method");
        String bodyTemplate = ctx.getConfigStr("bodyTemplate");

        if (url == null) {
            throw new IllegalArgumentException("Webhook 渠道配置不完整：url 不能为空");
        }

        // 渲染消息体（简单占位符替换）
        String body = renderTemplate(bodyTemplate != null ? bodyTemplate :
                "{\"text\":\"[${severity}] ${alertName}: ${summary}\"}", ctx);

        HttpMethod httpMethod = "GET".equalsIgnoreCase(method) ? HttpMethod.GET : HttpMethod.POST;

        String response = webClientBuilder.build()
                .method(httpMethod)
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(10))
                .block();

        log.info("Webhook 通知发送成功: url={}, response={}", url, response);
    }

    /**
     * 简单模板渲染（${变量名} 替换）
     */
    private String renderTemplate(String template, NotifyContext ctx) {
        if (template == null) return "";
        return template
                .replace("${alertName}", safe(ctx.getAlertName()))
                .replace("${severity}", safe(ctx.getSeverity()))
                .replace("${instance}", safe(ctx.getInstance()))
                .replace("${summary}", safe(ctx.getSummary()))
                .replace("${firedAt}", safe(ctx.getFiredAt()))
                .replace("${resolvedAt}", safe(ctx.getResolvedAt()))
                .replace("${recordId}", ctx.getAlertRecordId() != null ? ctx.getAlertRecordId().toString() : "");
    }

    private String safe(String val) {
        return val != null ? val : "";
    }
}
