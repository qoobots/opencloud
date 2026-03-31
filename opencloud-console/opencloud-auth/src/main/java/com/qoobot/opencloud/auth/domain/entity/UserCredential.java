package com.qoobot.opencloud.auth.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户凭证实体 —— 对应 auth_user_credential 表
 */
@Data
@TableName("auth_user_credential")
@Schema(description = "用户凭证")
public class UserCredential implements Serializable {

    @TableId(type = IdType.ASSIGN_UUID)
    @Schema(description = "主键 ID")
    private String id;

    @Schema(description = "关联 sys_user.id")
    private String userId;

    @Schema(description = "租户 ID")
    private String tenantId;

    @Schema(description = "BCrypt 加密密码")
    private String passwordHash;

    /** 枚举值：ACTIVE / DISABLED / LOCKED */
    @Schema(description = "账号状态")
    private String status;

    @Schema(description = "连续登录失败次数")
    private Integer loginFailCount;

    @Schema(description = "锁定到期时间（null 表示未锁定）")
    private LocalDateTime lockExpireTime;

    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginTime;

    @Schema(description = "密码最后修改时间")
    private LocalDateTime passwordUpdateTime;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
