package com.qoobot.opencloud.system.profile.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 个人信息 VO
 */
@Data
@Schema(description = "个人信息")
public class ProfileVO {

    @Schema(description = "用户 ID")
    private String id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号（脱敏）")
    private String phone;

    @Schema(description = "头像 URL")
    private String avatar;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "角色名称列表")
    private List<String> roleNames;

    @Schema(description = "权限标识列表")
    private List<String> permissions;
}
