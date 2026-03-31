package com.qoobot.opencloud.auth.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 修改密码请求 DTO
 */
@Data
@Schema(description = "修改密码请求")
public class ChangePasswordRequest {

    @NotBlank(message = "旧密码不能为空")
    @Schema(description = "旧密码")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Schema(description = "新密码（≥8位，含大小写字母和数字）")
    private String newPassword;

    @NotBlank(message = "确认密码不能为空")
    @Schema(description = "确认新密码")
    private String confirmPassword;
}
