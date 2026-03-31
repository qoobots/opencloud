package com.qoobot.opencloud.monitor.notify.sender.impl;

import com.qoobot.opencloud.monitor.notify.sender.NotifyContext;
import com.qoobot.opencloud.monitor.notify.sender.NotifySender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 钉钉 Webhook 通知发送器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DingTalkNotifySender implements NotifySender {

    private final WebClient.Builder webClientBuilder;

    @Override
    public String supportType() {
        return "DINGTALK";
    }

    @Override
    public void send(NotifyContext ctx) throws Exception {
        String webhookUrl = ctx.getConfigStr("webhookUrl");
        String secret = ctx.getConfigStr("secret");
        boolean isAtAll = ctx.getConfigBool("isAtAll", false);

        if (webhookUrl == null) {
            throw new IllegalArgumentException("钉钉渠道配置不完整：webhookUrl 不能为空");
        }

        // 加签
        String signedUrl = buildSignedUrl(webhookUrl, secret);

        // 构建消息体
        Map<String, Object> body = buildMarkdownBody(ctx, isAtAll);

        String response = webClientBuilder.build()
                .post()
                .uri(signedUrl)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(10))
                .block();

        log.info("钉钉通知发送成功: url={}, response={}", webhookUrl, response);
    }

    /**
     * 钉钉加签：timestamp + "\n" + secret 做 HMAC-SHA256，Base64 后 URLEncode
     */
    private String buildSignedUrl(String webhookUrl, String secret) {
        if (secret == null || secret.isEmpty()) {
            return webhookUrl;
        }
        try {
            long timestamp = System.currentTimeMillis();
            String stringToSign = timestamp + "\n" + secret;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
            String sign = URLEncoder.encode(Base64.getEncoder().encodeToString(signData), StandardCharsets.UTF_8);
            return webhookUrl + "&timestamp=" + timestamp + "&sign=" + sign;
        } catch (Exception e) {
            throw new RuntimeException("钉钉加签失败", e);
        }
    }

    private Map<String, Object> buildMarkdownBody(NotifyContext ctx, boolean isAtAll) {
        String title;
        String text;

        if (ctx.isTest()) {
            title = "[OpenCloud] 测试通知";
            text = "## 测试通知\n\n" + ctx.getTestMessage();
        } else if (ctx.isResolved()) {
            title = "✅ 告警已恢复: " + ctx.getAlertName();
            text = "## ✅ 告警已恢复\n\n" +
                   "**规则名称：**" + ctx.getAlertName() + "\n\n" +
                   "**实例地址：**" + ctx.getInstance() + "\n\n" +
                   "**解除时间：**" + ctx.getResolvedAt();
        } else {
            String icon = "CRITICAL".equals(ctx.getSeverity()) ? "🔴" : "WARNING".equals(ctx.getSeverity()) ? "🟡" : "🔵";
            title = icon + " [" + ctx.getSeverity() + "] " + ctx.getAlertName();
            text = "## " + icon + " 告警触发\n\n" +
                   "**规则名称：**" + ctx.getAlertName() + "\n\n" +
                   "**严重级别：**" + ctx.getSeverity() + "\n\n" +
                   "**实例地址：**" + ctx.getInstance() + "\n\n" +
                   "**告警摘要：**" + ctx.getSummary() + "\n\n" +
                   "**触发时间：**" + ctx.getFiredAt();
        }

        Map<String, Object> markdown = new HashMap<>();
        markdown.put("title", title);
        markdown.put("text", text);

        Map<String, Object> at = new HashMap<>();
        at.put("isAtAll", isAtAll);

        Map<String, Object> body = new HashMap<>();
        body.put("msgtype", "markdown");
        body.put("markdown", markdown);
        body.put("at", at);

        return body;
    }
}
