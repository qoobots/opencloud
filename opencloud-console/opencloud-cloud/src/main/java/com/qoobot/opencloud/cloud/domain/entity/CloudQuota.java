package com.qoobot.opencloud.cloud.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 配额配置实体
 * 对应表: cloud.cloud_quota
 */
@Data
@TableName("cloud.cloud_quota")
public class CloudQuota {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String tenantId;

    private Integer openstackVcpuLimit;

    private Long openstackMemoryLimit;

    private Integer openstackInstanceLimit;

    private Integer openstackVolumeCountLimit;

    private Long openstackVolumeStorageLimit;

    private Long cephStorageLimit;

    private Integer k8sCpuLimit;

    private Long k8sMemoryLimit;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
