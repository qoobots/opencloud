package com.qoobot.opencloud.monitor.notify.channel.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 通知渠道 VO（敏感字段脱敏）
 */
@Data
public class NotifyChannelVO {

    private Long id;

    private String tenantId;

    private String channelName;

    private String channelType;

    /** 脱敏后的配置（密码、secret 等替换为 ****） */
    private Map<String, Object> config;

    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
