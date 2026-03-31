package com.qoobot.opencloud.monitor.notify.sender.impl;

import com.qoobot.opencloud.monitor.notify.sender.NotifyContext;
import com.qoobot.opencloud.monitor.notify.sender.NotifySender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

/**
 * 邮件通知发送器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotifySender implements NotifySender {

    @Override
    public String supportType() {
        return "EMAIL";
    }

    @Override
    public void send(NotifyContext ctx) throws Exception {
        // 从解密后的 config 中获取邮件配置
        String smtpHost = ctx.getConfigStr("smtpHost");
        int smtpPort = ctx.getConfigInt("smtpPort", 465);
        String smtpUser = ctx.getConfigStr("smtpUser");
        String smtpPass = ctx.getConfigStr("smtpPass");
        String senderAddr = ctx.getConfigStr("senderAddr");
        String receiverAddrs = ctx.getConfigStr("receiverAddrs");
        boolean sslEnabled = ctx.getConfigBool("sslEnabled", true);

        if (smtpHost == null || senderAddr == null || receiverAddrs == null) {
            throw new IllegalArgumentException("邮件渠道配置不完整：smtpHost/senderAddr/receiverAddrs 不能为空");
        }

        // 动态构建 JavaMailSender
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(smtpHost);
        mailSender.setPort(smtpPort);
        mailSender.setUsername(smtpUser);
        mailSender.setPassword(smtpPass);
        mailSender.setDefaultEncoding("UTF-8");

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        if (sslEnabled) {
            props.put("mail.smtp.ssl.enable", "true");
        } else {
            props.put("mail.smtp.starttls.enable", "true");
        }

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(senderAddr);
        helper.setTo(receiverAddrs.split(","));
        helper.setSubject(buildSubject(ctx));
        helper.setText(buildHtmlBody(ctx), true);

        mailSender.send(message);
        log.info("邮件通知发送成功: to={}, subject={}", receiverAddrs, buildSubject(ctx));
    }

    private String buildSubject(NotifyContext ctx) {
        if (ctx.isTest()) {
            return "[OpenCloud] 测试通知";
        }
        if (ctx.isResolved()) {
            return String.format("[已恢复] %s - %s", ctx.getAlertName(), ctx.getInstance());
        }
        return String.format("[%s] %s - %s", ctx.getSeverity(), ctx.getAlertName(), ctx.getInstance());
    }

    private String buildHtmlBody(NotifyContext ctx) {
        if (ctx.isTest()) {
            return "<html><body><p>" + ctx.getTestMessage() + "</p></body></html>";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family: Arial, sans-serif;'>");
        sb.append("<h2 style='color: ").append(getSeverityColor(ctx.getSeverity())).append(";'>");
        sb.append(ctx.isResolved() ? "✅ 告警已恢复" : "🚨 告警触发").append("</h2>");
        sb.append("<table border='1' cellpadding='8' cellspacing='0' style='border-collapse:collapse;'>");
        sb.append("<tr><td><b>规则名称</b></td><td>").append(ctx.getAlertName()).append("</td></tr>");
        sb.append("<tr><td><b>严重级别</b></td><td>").append(ctx.getSeverity()).append("</td></tr>");
        sb.append("<tr><td><b>实例地址</b></td><td>").append(ctx.getInstance()).append("</td></tr>");
        sb.append("<tr><td><b>告警摘要</b></td><td>").append(ctx.getSummary()).append("</td></tr>");
        if (ctx.isResolved()) {
            sb.append("<tr><td><b>解除时间</b></td><td>").append(ctx.getResolvedAt()).append("</td></tr>");
        } else {
            sb.append("<tr><td><b>触发时间</b></td><td>").append(ctx.getFiredAt()).append("</td></tr>");
        }
        sb.append("</table>");
        sb.append("<p style='color:#999;font-size:12px;'>此邮件由 OpenCloud 自动发送，请勿回复。</p>");
        sb.append("</body></html>");
        return sb.toString();
    }

    private String getSeverityColor(String severity) {
        if ("CRITICAL".equals(severity)) return "#d32f2f";
        if ("WARNING".equals(severity)) return "#f57c00";
        return "#1976d2";
    }
}
