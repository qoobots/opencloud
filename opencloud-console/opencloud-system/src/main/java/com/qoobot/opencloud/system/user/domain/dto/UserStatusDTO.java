package com.qoobot.opencloud.system.user.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 切换用户状态 DTO
 */
@Data
@Schema(description = "切换用户状态请求")
public class UserStatusDTO {

    @NotBlank(message = "状态不能为空")
    @Schema(description = "状态：ACTIVE / DISABLED", requiredMode = Schema.RequiredMode.REQUIRED)
    private String status;
}
