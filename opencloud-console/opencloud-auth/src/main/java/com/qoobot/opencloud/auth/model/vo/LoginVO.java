package com.qoobot.opencloud.auth.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.Set;

/**
 * 登录响应 VO
 */
@Data
@Builder
@Schema(description = "登录响应")
public class LoginVO {

    @Schema(description = "访问 Token")
    private String accessToken;

    @Schema(description = "刷新 Token")
    private String refreshToken;

    @Schema(description = "Token 类型", example = "Bearer")
    private String tokenType;

    @Schema(description = "过期时间（秒）")
    private Long expiresIn;

    @Schema(description = "用户 ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "角色编码集合")
    private Set<String> roles;

    @Schema(description = "权限标识集合")
    private Set<String> permissions;
}
