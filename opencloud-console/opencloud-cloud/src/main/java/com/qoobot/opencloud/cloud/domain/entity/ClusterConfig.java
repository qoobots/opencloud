package com.qoobot.opencloud.cloud.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 集群配置实体
 * 对应表: cloud.cloud_cluster_config
 */
@Data
@TableName("cloud.cloud_cluster_config")
public class ClusterConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String tenantId;

    private String name;

    private String type;

    private String endpoint;

    private String configJson;

    private String status;

    private LocalDateTime lastCheckTime;

    private String errorMsg;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String createdBy;

    private String updatedBy;

    @TableLogic
    private Integer deleted;
}
