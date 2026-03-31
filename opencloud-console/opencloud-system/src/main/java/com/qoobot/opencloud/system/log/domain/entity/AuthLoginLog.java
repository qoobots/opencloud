package com.qoobot.opencloud.system.log.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 登录日志只读映射（跨 Schema 访问 auth.auth_login_log）
 * 只做查询，不做 INSERT/UPDATE/DELETE
 */
@Data
@TableName("auth.auth_login_log")
@Schema(description = "登录日志（只读，源自 auth 模块）")
public class AuthLoginLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "用户 ID")
    private String userId;

    @Schema(description = "租户 ID")
    private String tenantId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "登录 IP")
    private String loginIp;

    @Schema(description = "登录状态：SUCCESS / FAILED")
    private String status;

    @Schema(description = "失败原因")
    private String failReason;

    @Schema(description = "JWT JTI（登录成功才有值）")
    private String jti;

    @Schema(description = "登录时间")
    private LocalDateTime loginTime;

    @Schema(description = "登出时间")
    private LocalDateTime logoutTime;
}
