package com.qoobot.opencloud.monitor.alert.rule.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 告警规则实体
 */
@Data
@TableName("monitor_alert_rule")
public class MonitorAlertRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String tenantId;

    private String ruleName;

    private String description;

    private String promqlExpr;

    private String severity;

    private String duration;

    private String notifyChannelIds;

    private Boolean notifyOnResolve;

    private String status;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String createdBy;

    private String updatedBy;
}
