package com.qoobot.opencloud.cloud.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资源同步日志实体
 * 对应表: cloud.cloud_sync_log
 */
@Data
@TableName("cloud.cloud_sync_log")
public class CloudSyncLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String clusterId;

    private String tenantId;

    private String syncType;

    private String status;

    private String resourceCounts;

    private String errorMsg;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private LocalDateTime createdAt;
}
