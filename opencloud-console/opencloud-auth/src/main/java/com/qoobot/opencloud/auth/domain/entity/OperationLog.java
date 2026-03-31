package com.qoobot.opencloud.auth.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作审计日志实体 —— 对应 auth_operation_log 表
 */
@Data
@TableName("auth_operation_log")
@Schema(description = "操作审计日志")
public class OperationLog implements Serializable {

    @TableId(type = IdType.ASSIGN_UUID)
    @Schema(description = "主键 ID")
    private String id;

    @Schema(description = "操作人用户 ID")
    private String operatorId;

    @Schema(description = "被操作用户 ID")
    private String targetUserId;

    @Schema(description = "操作类型：CHANGE_PASSWORD / FORCE_LOGOUT / UNLOCK_ACCOUNT")
    private String operationType;

    @Schema(description = "备注")
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "操作时间")
    private LocalDateTime createdAt;
}
