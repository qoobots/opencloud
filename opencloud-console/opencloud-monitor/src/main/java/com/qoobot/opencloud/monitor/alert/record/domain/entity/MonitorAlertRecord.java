package com.qoobot.opencloud.monitor.alert.record.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 告警记录实体
 */
@Data
@TableName("monitor_alert_record")
public class MonitorAlertRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String tenantId;

    private Long ruleId;

    private String ruleName;

    private String alertName;

    private String severity;

    private String instance;

    private String labels;

    private String annotations;

    private String summary;

    private String status;

    private LocalDateTime firedAt;

    private LocalDateTime resolvedAt;

    private String ackBy;

    private LocalDateTime ackAt;

    private String ackNote;

    private Boolean notified;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
