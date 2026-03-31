package com.qoobot.opencloud.auth.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Token 刷新响应 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Token 刷新响应")
public class TokenRefreshVO {

    @Schema(description = "新 Access Token")
    private String accessToken;

    @Schema(description = "Access Token 过期时间")
    private LocalDateTime accessTokenExpireAt;

    @Schema(description = "新 Refresh Token")
    private String refreshToken;

    @Schema(description = "Refresh Token 过期时间")
    private LocalDateTime refreshTokenExpireAt;
}
