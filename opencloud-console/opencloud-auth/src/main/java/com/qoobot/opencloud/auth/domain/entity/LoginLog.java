package com.qoobot.opencloud.auth.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 登录日志实体 —— 对应 auth_login_log 表
 */
@Data
@TableName("auth_login_log")
@Schema(description = "登录日志")
public class LoginLog implements Serializable {

    @TableId(type = IdType.ASSIGN_UUID)
    @Schema(description = "主键 ID")
    private String id;

    @Schema(description = "用户 ID")
    private String userId;

    @Schema(description = "租户 ID")
    private String tenantId;

    @Schema(description = "登录 IP")
    private String loginIp;

    @Schema(description = "登录状态：SUCCESS / FAILED / LOGOUT / FORCE_LOGOUT")
    private String status;

    @Schema(description = "失败原因（登录成功时为 null）")
    private String failReason;

    @Schema(description = "User-Agent")
    private String userAgent;

    @Schema(description = "本次登录关联的 AccessToken jti")
    private String jti;

    @Schema(description = "登出时间")
    private LocalDateTime logoutTime;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "登录时间")
    private LocalDateTime createdAt;
}
