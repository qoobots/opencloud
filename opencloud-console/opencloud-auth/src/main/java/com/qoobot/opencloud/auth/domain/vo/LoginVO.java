package com.qoobot.opencloud.auth.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 登录响应 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "登录响应")
public class LoginVO {

    @Schema(description = "Access Token（JWT）")
    private String accessToken;

    @Schema(description = "Access Token 过期时间")
    private LocalDateTime accessTokenExpireAt;

    @Schema(description = "Refresh Token（UUID）")
    private String refreshToken;

    @Schema(description = "Refresh Token 过期时间")
    private LocalDateTime refreshTokenExpireAt;

    @Schema(description = "Token 类型", example = "Bearer")
    private String tokenType;

    @Schema(description = "用户基础信息")
    private UserInfoVO userInfo;
}
