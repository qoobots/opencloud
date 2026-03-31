package com.qoobot.opencloud.auth.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求 DTO
 */
@Data
@Schema(description = "登录请求")
public class LoginRequest {

    @NotBlank(message = "用户名不能为空")
    @Schema(description = "用户名", example = "admin")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码", example = "Admin@123")
    private String password;

    @Schema(description = "验证码 Key（captcha-enabled=true 时必填）")
    private String captchaKey;

    @Schema(description = "验证码文本（captcha-enabled=true 时必填）")
    private String captchaCode;
}
