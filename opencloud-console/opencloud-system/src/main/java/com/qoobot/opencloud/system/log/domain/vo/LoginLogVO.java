package com.qoobot.opencloud.system.log.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录日志 VO
 */
@Data
@Schema(description = "登录日志")
public class LoginLogVO {

    @Schema(description = "日志 ID")
    private Long id;

    @Schema(description = "用户 ID")
    private String userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "登录 IP")
    private String loginIp;

    @Schema(description = "登录状态：SUCCESS / FAILED")
    private String status;

    @Schema(description = "失败原因")
    private String failReason;

    @Schema(description = "登录时间")
    private LocalDateTime loginTime;

    @Schema(description = "登出时间")
    private LocalDateTime logoutTime;
}
