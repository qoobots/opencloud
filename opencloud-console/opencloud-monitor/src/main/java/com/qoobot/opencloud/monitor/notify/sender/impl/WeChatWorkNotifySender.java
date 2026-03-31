package com.qoobot.opencloud.monitor.notify.sender.impl;

import com.qoobot.opencloud.monitor.notify.sender.NotifyContext;
import com.qoobot.opencloud.monitor.notify.sender.NotifySender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 企业微信机器人通知发送器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeChatWorkNotifySender implements NotifySender {

    private final WebClient.Builder webClientBuilder;

    @Override
    public String supportType() {
        return "WECHAT_WORK";
    }

    @Override
    public void send(NotifyContext ctx) throws Exception {
        String webhookUrl = ctx.getConfigStr("webhookUrl");

        if (webhookUrl == null) {
            throw new IllegalArgumentException("企业微信渠道配置不完整：webhookUrl 不能为空");
        }

        Map<String, Object> body = buildMarkdownBody(ctx);

        String response = webClientBuilder.build()
                .post()
                .uri(webhookUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(10))
                .block();

        log.info("企业微信通知发送成功: response={}", response);
    }

    private Map<String, Object> buildMarkdownBody(NotifyContext ctx) {
        String content;

        if (ctx.isTest()) {
            content = "## 测试通知\n\n" + ctx.getTestMessage();
        } else if (ctx.isResolved()) {
            content = "## <font color=\"info\">✅ 告警已恢复</font>\n\n" +
                      "> 规则名称：<font color=\"comment\">" + ctx.getAlertName() + "</font>\n" +
                      "> 实例地址：" + ctx.getInstance() + "\n" +
                      "> 解除时间：" + ctx.getResolvedAt();
        } else {
            String color = "CRITICAL".equals(ctx.getSeverity()) ? "warning" :
                           "WARNING".equals(ctx.getSeverity()) ? "warning" : "info";
            content = "## <font color=\"" + color + "\">🚨 告警触发</font>\n\n" +
                      "> 规则名称：<font color=\"comment\">" + ctx.getAlertName() + "</font>\n" +
                      "> 严重级别：**" + ctx.getSeverity() + "**\n" +
                      "> 实例地址：" + ctx.getInstance() + "\n" +
                      "> 告警摘要：" + ctx.getSummary() + "\n" +
                      "> 触发时间：" + ctx.getFiredAt();
        }

        Map<String, Object> markdown = new HashMap<>();
        markdown.put("content", content);

        Map<String, Object> body = new HashMap<>();
        body.put("msgtype", "markdown");
        body.put("markdown", markdown);

        return body;
    }
}
