package com.qoobot.opencloud.system.user.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 重置用户密码 DTO
 */
@Data
@Schema(description = "重置用户密码请求")
public class UserResetPasswordDTO {

    @NotBlank(message = "新密码不能为空")
    @Schema(description = "新密码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String newPassword;
}
