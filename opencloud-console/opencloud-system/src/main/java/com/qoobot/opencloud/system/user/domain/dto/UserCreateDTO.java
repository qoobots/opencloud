package com.qoobot.opencloud.system.user.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 新增用户 DTO
 */
@Data
@Schema(description = "新增用户请求")
public class UserCreateDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(max = 64, message = "用户名最长 64 字符")
    @Schema(description = "登录账号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank(message = "昵称不能为空")
    @Size(max = 64, message = "昵称最长 64 字符")
    @Schema(description = "显示昵称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nickname;

    @NotBlank(message = "初始密码不能为空")
    @Schema(description = "初始密码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "所属部门 ID")
    private Long deptId;

    @Schema(description = "角色 ID 列表")
    private List<Long> roleIds;

    @Schema(description = "状态：ACTIVE / DISABLED", defaultValue = "ACTIVE")
    private String status;

    @Schema(description = "排序", defaultValue = "0")
    private Integer sort;
}
