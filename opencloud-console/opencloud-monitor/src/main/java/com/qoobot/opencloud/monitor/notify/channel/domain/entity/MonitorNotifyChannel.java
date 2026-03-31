package com.qoobot.opencloud.monitor.notify.channel.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知渠道实体
 */
@Data
@TableName("monitor_notify_channel")
public class MonitorNotifyChannel {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String tenantId;

    private String channelName;

    /** 渠道类型：EMAIL / DINGTALK / WECHAT_WORK / WEBHOOK */
    private String channelType;

    /** 渠道配置 JSON（敏感字段 AES-256-GCM 加密存储） */
    private String config;

    private String description;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String createdBy;

    private String updatedBy;
}
