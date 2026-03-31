package com.qoobot.opencloud.monitor.notify.log.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知发送日志实体
 */
@Data
@TableName("monitor_notify_log")
public class MonitorNotifyLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String tenantId;

    private Long alertRecordId;

    private Long channelId;

    private String channelName;

    private String channelType;

    private Boolean isTest;

    /** SUCCESS / FAILED */
    private String status;

    private String errorMsg;

    private Integer retryCount;

    private LocalDateTime sentAt;
}
