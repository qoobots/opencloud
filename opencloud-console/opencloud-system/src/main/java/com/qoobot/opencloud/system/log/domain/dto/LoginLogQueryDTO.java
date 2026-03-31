package com.qoobot.opencloud.system.log.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 登录日志查询条件 DTO
 */
@Data
@Schema(description = "登录日志查询条件")
public class LoginLogQueryDTO {

    @Schema(description = "用户名（模糊）")
    private String username;

    @Schema(description = "登录状态：SUCCESS / FAILED")
    private String status;

    @Schema(description = "登录 IP（模糊）")
    private String loginIp;

    @Schema(description = "开始时间")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime beginTime;

    @Schema(description = "结束时间")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endTime;
}
