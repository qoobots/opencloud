package com.qoobot.opencloud.cloud.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 异步任务实体
 * 对应表: cloud.cloud_task
 */
@Data
@TableName("cloud.cloud_task")
public class CloudTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskId;

    private String tenantId;

    private String type;

    private String status;

    private String resourceType;

    private String resourceId;

    private String clusterId;

    private Integer progress;

    private String resultJson;

    private String errorMsg;

    private LocalDateTime createdAt;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private String createdBy;
}
