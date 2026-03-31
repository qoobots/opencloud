package com.qoobot.opencloud.monitor.notify.channel.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.Map;

/**
 * 通知渠道创建 DTO
 */
@Data
public class NotifyChannelCreateDTO {

    @NotBlank(message = "渠道名称不能为空")
    @Size(max = 100, message = "渠道名称长度不能超过100")
    private String channelName;

    @NotBlank(message = "渠道类型不能为空")
    @Pattern(regexp = "^(EMAIL|DINGTALK|WECHAT_WORK|WEBHOOK)$",
             message = "渠道类型必须是 EMAIL、DINGTALK、WECHAT_WORK 或 WEBHOOK")
    private String channelType;

    /**
     * 渠道配置（各类型字段不同）
     * EMAIL: smtpHost, smtpPort, smtpUser, smtpPass, senderAddr, receiverAddrs, sslEnabled
     * DINGTALK: webhookUrl, secret, atMobiles, isAtAll
     * WECHAT_WORK: webhookUrl, mentionedList
     * WEBHOOK: url, method, headers, bodyTemplate
     */
    private Map<String, Object> config;

    @Size(max = 300, message = "描述长度不能超过300")
    private String description;
}
