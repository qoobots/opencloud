package com.qoobot.opencloud.auth.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Token 刷新请求 DTO
 */
@Data
@Schema(description = "Token 刷新请求")
public class TokenRefreshRequest {

    @NotBlank(message = "refreshToken 不能为空")
    @Schema(description = "Refresh Token", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
    private String refreshToken;
}
