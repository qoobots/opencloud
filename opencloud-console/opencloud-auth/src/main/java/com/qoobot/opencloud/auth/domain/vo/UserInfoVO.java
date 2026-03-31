package com.qoobot.opencloud.auth.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * 用户基础信息 VO（登录响应和 /me 接口共用）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户信息")
public class UserInfoVO {

    @Schema(description = "用户 ID")
    private String userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像 URL")
    private String avatar;

    @Schema(description = "租户 ID")
    private String tenantId;

    @Schema(description = "租户名称")
    private String tenantName;

    @Schema(description = "角色编码集合")
    private Set<String> roles;

    @Schema(description = "权限标识集合")
    private Set<String> permissions;
}
