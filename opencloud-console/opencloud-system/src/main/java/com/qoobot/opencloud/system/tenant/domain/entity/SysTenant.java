package com.qoobot.opencloud.system.tenant.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 租户表
 */
@Data
@TableName("sys_tenant")
@Schema(description = "系统租户")
public class SysTenant implements Serializable {

    @Schema(description = "租户 ID（UUID）")
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    @Schema(description = "租户编码（全局唯一，创建后不可修改）")
    private String tenantCode;

    @Schema(description = "租户名称")
    private String tenantName;

    @Schema(description = "联系人姓名")
    private String contactName;

    @Schema(description = "联系人邮箱")
    private String contactEmail;

    @Schema(description = "状态：ACTIVE / DISABLED")
    private String status;

    @Schema(description = "是否默认租户（不可删除）")
    private Boolean isDefault;

    @Schema(description = "有效期（NULL=永久有效）")
    private LocalDateTime expireTime;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @Schema(description = "逻辑删除")
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
